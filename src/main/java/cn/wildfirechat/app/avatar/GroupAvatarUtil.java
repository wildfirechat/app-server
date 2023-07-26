package cn.wildfirechat.app.avatar;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: fangdaji
 * @date: 2019/3/23 15:59
 * @description:
 */
public class GroupAvatarUtil {
    public static void getCombinationOfHead(List<URL> paths, File targetFile)
        throws IOException, URISyntaxException {

        List<BufferedImage> bufferedImages = new ArrayList<BufferedImage>();
        // 压缩图片所有的图片生成尺寸同意的 为 50x50

        int imageSize = 33;
        if (paths.size() <= 4) {
            imageSize = 50;
        }

        for (int i = 0; i < paths.size(); i++) {
            bufferedImages.add(resize2(paths.get(i), imageSize, imageSize, true));
        }

        int width = 112; // 这是画板的宽高

        int height = 112; // 这是画板的高度

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
                    g2d.drawImage(bufferedImages.get(i - 1), 33 * i + 3 * i - 33, 4, null);
                } else if (i <= 6) {
                    g2d.drawImage(bufferedImages.get(i - 1), 33 * j + 3 * j - 33, 41, null);
                    j++;
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 33 * k + 3 * k - 33, 77, null);
                    k++;
                }
            } else if (bufferedImages.size() == 8) {
                if (i <= 2) {
                    g2d.drawImage(bufferedImages.get(i - 1), 33 * i + 4 * i - 18, 4, null);
                } else if (i <= 5) {
                    g2d.drawImage(bufferedImages.get(i - 1), 33 * j + 3 * j - 33, 41, null);
                    j++;
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 33 * k + 3 * k - 33, 77, null);
                    k++;
                }
            } else if (bufferedImages.size() == 7) {
                if (i <= 1) {
                    g2d.drawImage(bufferedImages.get(i - 1), 39, 4, null);
                } else if (i <= 4) {
                    g2d.drawImage(bufferedImages.get(i - 1), 33 * j + 3 * j - 33, 41, null);
                    j++;
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 33 * k + 3 * k - 33, 77, null);
                    k++;
                }
            } else if (bufferedImages.size() == 6) {
                if (i <= 3) {
                    g2d.drawImage(bufferedImages.get(i - 1), 33 * i + 3 * i - 33, 15, null);
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 33 * j + 3 * j - 33, 58, null);
                    j++;
                }
            } else if (bufferedImages.size() == 5) {
                if (i <= 2) {
                    g2d.drawImage(bufferedImages.get(i - 1), 33 * i + 4 * i - 18, 15, null);
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 33 * j + 3 * j - 33, 58, null);
                    j++;
                }
            } else if (bufferedImages.size() == 4) {
                if (i <= 2) {
                    g2d.drawImage(bufferedImages.get(i - 1), 50 * i + 4 * i - 50, 4, null);
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 50 * j + 4 * j - 50, 58, null);
                    j++;
                }
            } else if (bufferedImages.size() == 3) {
                if (i <= 1) {
                    g2d.drawImage(bufferedImages.get(i - 1), 31, 4, null);
                } else {
                    g2d.drawImage(bufferedImages.get(i - 1), 50 * j + 4 * j - 50, 58, null);
                    j++;
                }

            } else if (bufferedImages.size() == 2) {

                g2d.drawImage(bufferedImages.get(i - 1), 50 * i + 4 * i - 50,
                    31, null);

            } else if (bufferedImages.size() == 1) {

                g2d.drawImage(bufferedImages.get(i - 1), 31, 31, null);

            }

            // 需要改变颜色的话在这里绘上颜色。可能会用到AlphaComposite类
        }


        String format = "png";
        ImageIO.write(outImage, format, targetFile);
    }

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
        DataInputStream dis = null;
        try {
            URLConnection urlConnection = filePath.openConnection();
            urlConnection.setConnectTimeout(5 * 1000);
            urlConnection.setReadTimeout(5 * 1000);
            dis = new DataInputStream(urlConnection.getInputStream());

            double ratio = 0; // 缩放比例
            //File f = new File(dis);
            BufferedImage bi = ImageIO.read(dis);
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
            e.printStackTrace();
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
