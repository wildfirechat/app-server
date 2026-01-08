package cn.wildfirechat.app.slide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SlideVerifyService {
    private static final Logger LOG = LoggerFactory.getLogger(SlideVerifyService.class);

    // 存储验证码信息：token -> 验证数据
    private final ConcurrentHashMap<String, VerifyData> verifyCache = new ConcurrentHashMap<>();

    // 验证码有效时间（秒）
    private static final int VERIFY_TIMEOUT = 300; // 5分钟

    // 验证误差范围（像素）
    private static final int TOLERANCE = 5;

    // 图片尺寸
    private static final int IMAGE_WIDTH = 300;
    private static final int IMAGE_HEIGHT = 150;
    private static final int SLIDER_WIDTH = 50;
    private static final int SLIDER_HEIGHT = 50;

    // 验证数据类
    private static class VerifyData {
        int x;  // 正确的x坐标
        long timestamp; // 生成时间戳
        boolean verified; // 是否已验证

        VerifyData(int x) {
            this.x = x;
            this.timestamp = System.currentTimeMillis();
            this.verified = false;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > VERIFY_TIMEOUT * 1000;
        }
    }

    /**
     * 生成滑动验证码
     */
    public Map<String, Object> generateSlideVerify() {
        // 生成token
        String token = UUID.randomUUID().toString();

        // 随机生成滑块的x和y坐标
        int x = 50 + (int)(Math.random() * (IMAGE_WIDTH - SLIDER_WIDTH - 100)); // 留出左右余量
        int y = 20 + (int)(Math.random() * (IMAGE_HEIGHT - SLIDER_HEIGHT - 40)); // 留出上下余量

        LOG.info("生成滑动验证码: token={}, x={}, y={}", token, x, y);

        // 存储验证数据
        verifyCache.put(token, new VerifyData(x));

        try {
            // 生成背景图（带缺口）
            String backgroundImage = generateBackgroundWithHole(x, y);
            LOG.info("背景图生成完成，长度: {}", backgroundImage.length());

            // 生成滑块图
            String sliderImage = generateSlider(x, y);
            LOG.info("滑块图生成完成，长度: {}", sliderImage.length());

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("backgroundImage", backgroundImage);
            result.put("sliderImage", sliderImage);
            result.put("y", y);

            return result;
        } catch (Exception e) {
            LOG.error("生成滑动验证码失败", e);
            verifyCache.remove(token);
            throw new RuntimeException("生成滑动验证码失败");
        }
    }

    /**
     * 验证滑动位置
     */
    public boolean verifySlide(String token, int userX) {
        VerifyData data = verifyCache.get(token);

        if (data == null) {
            LOG.warn("验证token不存在: {}", token);
            return false;
        }

        if (data.isExpired()) {
            LOG.warn("验证token已过期: {}", token);
            verifyCache.remove(token);
            return false;
        }

        if (data.verified) {
            LOG.warn("验证token已使用: {}", token);
            return false;
        }

        // 验证位置是否在误差范围内
        boolean success = Math.abs(data.x - userX) <= TOLERANCE;

        if (success) {
            data.verified = true;
            LOG.info("滑动验证成功，token: {}, 正确位置: {}, 用户位置: {}", token, data.x, userX);
        } else {
            LOG.warn("滑动验证失败，token: {}, 正确位置: {}, 用户位置: {}", token, data.x, userX);
            verifyCache.remove(token); // 验证失败则移除token
        }

        return success;
    }

    /**
     * 检查token是否已验证（一次性使用）
     */
    public boolean isVerified(String token) {
        VerifyData data = verifyCache.get(token);
        if (data == null || data.isExpired()) {
            return false;
        }

        // token已验证通过，立即删除，确保只能使用一次
        if (data.verified) {
            LOG.info("验证token已使用，删除token: {}", token);
            verifyCache.remove(token);
            return true;
        }

        return false;
    }

    /**
     * 清理过期的验证数据
     */
    public void cleanExpiredData() {
        verifyCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 生成带缺口的背景图
     */
    private String generateBackgroundWithHole(int x, int y) throws IOException {
        // 创建背景图片
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制渐变背景
        GradientPaint gradient = new GradientPaint(0, 0, getRandomColor(), IMAGE_WIDTH, IMAGE_HEIGHT, getRandomColor());
        g.setPaint(gradient);
        g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // 添加一些随机图形增加复杂度
        addRandomShapes(g);

        // 绘制缺口（带阴影效果的镂空）
        drawHole(g, x, y);

        g.dispose();

        return imageToBase64(image);
    }

    /**
     * 生成滑块图
     */
    private String generateSlider(int x, int y) throws IOException {
        // 创建滑块图片（透明背景）
        BufferedImage image = new BufferedImage(SLIDER_WIDTH, SLIDER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // 设置抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制拼图形状的滑块
        drawSliderShape(g);

        g.dispose();

        return imageToBase64(image);
    }

    /**
     * 绘制缺口
     */
    private void drawHole(Graphics2D g, int x, int y) {
        // 使用半透明黑色绘制缺口
        g.setColor(new Color(0, 0, 0, 130));

        // 绘制拼图形状的缺口
        int w = SLIDER_WIDTH;
        int h = SLIDER_HEIGHT;

        // 使用圆角矩形绘制简单的缺口，不绘制边框
        RoundRectangle2D shape = new RoundRectangle2D.Float(x, y, w, h, 10, 10);
        g.fill(shape);
    }

    /**
     * 绘制滑块
     */
    private void drawSliderShape(Graphics2D g) {
        int w = SLIDER_WIDTH;
        int h = SLIDER_HEIGHT;

        // 绘制白色滑块，不绘制边框
        g.setColor(new Color(255, 255, 255, 250));
        RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w, h, 10, 10);
        g.fill(shape);

        // 在滑块中间添加箭头图标
        g.setColor(new Color(80, 80, 80));
        int arrowX = w / 2 - 8;
        int arrowY = h / 2 - 8;
        int[] xp = {arrowX, arrowX + 16, arrowX + 8};
        int[] yp = {arrowY, arrowY + 8, arrowY + 16};
        g.fillPolygon(xp, yp, 3);
    }

    /**
     * 添加随机图形
     */
    private void addRandomShapes(Graphics2D g) {
        for (int i = 0; i < 20; i++) {
            g.setColor(getRandomColor());
            int shapeType = (int)(Math.random() * 3);
            int x = (int)(Math.random() * IMAGE_WIDTH);
            int y = (int)(Math.random() * IMAGE_HEIGHT);
            int size = 10 + (int)(Math.random() * 30);

            switch (shapeType) {
                case 0:
                    g.fillOval(x, y, size, size);
                    break;
                case 1:
                    g.fillRect(x, y, size, size);
                    break;
                case 2:
                    g.drawLine(x, y, x + size, y + size);
                    break;
            }
        }
    }

    /**
     * 获取随机颜色
     */
    private Color getRandomColor() {
        return new Color(
            (int)(Math.random() * 256),
            (int)(Math.random() * 256),
            (int)(Math.random() * 256)
        );
    }

    /**
     * 将图片转换为Base64编码
     */
    private String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }
}
