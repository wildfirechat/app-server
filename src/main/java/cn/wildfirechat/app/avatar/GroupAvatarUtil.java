package cn.wildfirechat.app.avatar;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.input.BoundedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: fangdaji
 * @date: 2019/3/23 15:59
 * @description:
 */
public class GroupAvatarUtil {
    private static final Logger LOG = LoggerFactory.getLogger(GroupAvatarUtil.class);

    /** 单个头像响应体大小上限。 */
    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;

    /** 重定向最大跳数。 */
    private static final int MAX_REDIRECTS = 3;

    public static void getCombinationOfHead(List<URL> paths, File targetFile)
        throws IOException, URISyntaxException {

        List<BufferedImage> bufferedImages = new ArrayList<BufferedImage>();
        // 压缩图片所有的图片生成尺寸同意的 为 50x50

        int width = 112; // 这是画板的宽高
        int height = 112; // 这是画板的高度

        int imageSize = 36;
        if (paths.size() == 1) {
            imageSize = 112;
        } else if (paths.size() <= 4) {
            imageSize = 54;
        }

        for (int i = 0; i < paths.size(); i++) {
            bufferedImages.add(resize2(paths.get(i), imageSize, imageSize, true));
        }

        // BufferedImage.TYPE_INT_RGB可以自己定义可查看API

        BufferedImage outImage = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_RGB);

        // 生成画布
        Graphics g = outImage.getGraphics();

        Graphics2D g2d = (Graphics2D) g;

        // 设置背景色
        g2d.setBackground(new Color(231, 231, 231));
        //g2d.setBackground(new Color(231, 0, 4));

        // 通过使用当前绘图表面的背景色进行填充来清除指定的矩形。
        g2d.clearRect(0, 0, width, height);

        // 开始拼凑 根据图片的数量判断该生成那种样式的组合头像目前为4中
        int j = 1;
        int k = 1;
        for (int i = 1; i <= bufferedImages.size(); i++) {
            if (bufferedImages.size() == 9) {
                if (i <= 3) {
                    g2d.drawImage(bufferedImages.get(i - 1), 38 * (i - 1), 0, null);
                } else if (i <= 6) {
                    g2d.drawImage(bufferedImages.get(i - 1), 38 * (j - 1), 38, null);
                    j++;
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 38 * (k - 1), 76, null);
                    k++;
                }
            } else if (bufferedImages.size() == 8) {
                if (i <= 2) {
                    g2d.drawImage(bufferedImages.get(i - 1), 19 + 38 * (i - 1), 0, null);
                } else if (i <= 5) {
                    g2d.drawImage(bufferedImages.get(i - 1), 38 * (j - 1), 38, null);
                    j++;
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 38 * (k - 1), 76, null);
                    k++;
                }
            } else if (bufferedImages.size() == 7) {
                if (i <= 1) {
                    g2d.drawImage(bufferedImages.get(i - 1), 38, 0, null);
                } else if (i <= 4) {
                    g2d.drawImage(bufferedImages.get(i - 1), 38 * (j - 1), 38, null);
                    j++;
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 38 * (k - 1), 76, null);
                    k++;
                }
            } else if (bufferedImages.size() == 6) {
                if (i <= 3) {
                    g2d.drawImage(bufferedImages.get(i - 1), 38 * (i - 1), 19, null);
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 38 * (j - 1), 57, null);
                    j++;
                }
            } else if (bufferedImages.size() == 5) {
                if (i <= 2) {
                    g2d.drawImage(bufferedImages.get(i - 1), 19 + 38 * (i - 1), 19, null);
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 38 * (j - 1), 57, null);
                    j++;
                }
            } else if (bufferedImages.size() == 4) {
                if (i <= 2) {
                    g2d.drawImage(bufferedImages.get(i - 1), 58 * (i - 1), 0, null);
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 58 * (j - 1), 58, null);
                    j++;
                }
            } else if (bufferedImages.size() == 3) {
                if (i <= 1) {
                    g2d.drawImage(bufferedImages.get(i - 1), 29, 0, null);
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 58 * (j - 1), 58, null);
                    j++;
                }

            } else if (bufferedImages.size() == 2) {

                g2d.drawImage(bufferedImages.get(i - 1), 58 * (i - 1),
                    29, null);

            } else if (bufferedImages.size() == 1) {

                g2d.drawImage(bufferedImages.get(i - 1), 0, 0, null);

            }

            // 需要改变颜色的话在这里绘上颜色。可能会用到AlphaComposite类
        }


        String format = "png";
        ImageIO.write(outImage, format, targetFile);
    }

    private static final String DEFAULT_AVATAR_PATH = "/static/avatar/avatar_def.png";

    /**
     * 图片缩放
     *
     * @param filePath 图片路径
     * @param height   高度
     * @param width    宽度
     * @param bb       比例不对时是否需要补白
     */
    private static BufferedImage resize2(URL filePath, int height, int width,
                                         boolean bb) {
        return resize2(filePath, height, width, bb, true);
    }

    /**
     * 打开头像连接。重定向时逐跳重新检查地址；{@code file:} / {@code jar:} 仅用于内部资源。
     */
    private static URLConnection openConnectionSafely(URL url) throws IOException {
        URL current = url;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            String protocol = current.getProtocol() == null ? "" : current.getProtocol().toLowerCase();
            if ("file".equals(protocol) || "jar".equals(protocol)) {
                // 内部资源：file 是名字头像，jar 是 Spring Boot 可执行 jar 里的默认头像
                // （形如 jar:file:/...jar!/BOOT-INF/classes!/static/avatar/avatar_def.png）
                return current.openConnection();
            }
            if (!AvatarUrlValidator.isSafePublicUrl(current)) {
                throw new IOException("Unavailable avatar url: " + current);
            }

            HttpURLConnection conn = (HttpURLConnection) current.openConnection();
            conn.setConnectTimeout(5 * 1000);
            conn.setReadTimeout(5 * 1000);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Accept", "image/*");

            int code = conn.getResponseCode();
            boolean redirect = code == 301 || code == 302 || code == 303 || code == 307 || code == 308;
            if (redirect) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.trim().isEmpty()) {
                    throw new IOException("Redirect without Location: " + current);
                }
                current = new URL(current, location);
                continue;
            }
            return conn;
        }
        throw new IOException("Too many redirects for avatar: " + url);
    }

    private static BufferedImage resize2(URL filePath, int height, int width,
                                         boolean bb, boolean fallback) {
        DataInputStream dis = null;
        try {
            URLConnection urlConnection = openConnectionSafely(filePath);

            long contentLength = urlConnection.getContentLengthLong();
            if (contentLength > MAX_AVATAR_BYTES) {
                throw new IOException("Avatar too large: " + contentLength);
            }
            String contentType = urlConnection.getContentType();
            if (contentType != null) {
                String ct = contentType.toLowerCase();
                if (!ct.startsWith("image/") && !ct.startsWith("application/octet-stream")) {
                    throw new IOException("Not an image content type: " + contentType);
                }
            }
            dis = new DataInputStream(
                    new BoundedInputStream(urlConnection.getInputStream(), MAX_AVATAR_BYTES));

            double ratio = 0; // 缩放比例
            BufferedImage bi = ImageIO.read(dis);
            if (bi == null) {
                throw new IOException("Failed to read image from " + filePath);
            }
            Image itemp = bi.getScaledInstance(width, height,
                Image.SCALE_SMOOTH);
            // 计算比例
            if ((bi.getHeight() > height) || (bi.getWidth() > width)) {
                if (bi.getHeight() > bi.getWidth()) {
                    ratio = (new Integer(height)).doubleValue()
                        / bi.getHeight();
                } else {
                    ratio = (new Integer(width)).doubleValue() / bi.getWidth();
                }
                AffineTransformOp op = new AffineTransformOp(
                    AffineTransform.getScaleInstance(ratio, ratio), null);
                itemp = op.filter(bi, null);
            }
            if (bb) {
                // copyimg(filePath, "D:\\img");
                BufferedImage image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                g.setColor(Color.white);
                g.fillRect(0, 0, width, height);
                if (width == itemp.getWidth(null)) {
                    g.drawImage(itemp, 0, (height - itemp.getHeight(null)) / 2,
                        itemp.getWidth(null), itemp.getHeight(null),
                        Color.white, null);
                } else {
                    g.drawImage(itemp, (width - itemp.getWidth(null)) / 2, 0,
                        itemp.getWidth(null), itemp.getHeight(null),
                        Color.white, null);
                }
                g.dispose();
                itemp = image;
            }
            return (BufferedImage) itemp;
        } catch (IOException e) {
            LOG.error("load avatar failed, url: {}, fallback: {}", filePath, fallback, e);
            if (fallback) {
                URL defaultAvatarUrl = GroupAvatarUtil.class.getResource(DEFAULT_AVATAR_PATH);
                if (defaultAvatarUrl != null) {
                    return resize2(defaultAvatarUrl, height, width, bb, false);
                }
            }
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
        return null;
    }

}
