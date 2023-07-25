package cn.wildfirechat.app.avatar;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class NameAvatarBuilder {

    private BufferedImage templateImage;
    private Graphics2D templateG2D;
    private int templateWidth;
    private int templateHeight;

    private String fullName;

    public NameAvatarBuilder(String bgRGB) {
        //            templateImage = ImageIO.read(new File("./avatar/headbg.jpg"));
        templateImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        templateG2D = templateImage.createGraphics();
        templateWidth = templateImage.getWidth();
        templateHeight = templateImage.getHeight();
        templateG2D.setBackground(Color.decode(bgRGB));
        templateG2D.clearRect(0, 0, templateWidth, templateHeight);
    }

    public NameAvatarBuilder name(String drawName, String fullName) {
        this.fullName = fullName;
        int x = templateWidth * 3 / 10;
        int y = templateHeight * 2 / 3;
        // templateG2D.translate(0, 100);
        // g2.rotate(Math.PI / 3);
        templateG2D.setFont(templateG2D.getFont().deriveFont(40f));
        templateG2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        // templateG2D.setColor(Color.WHITE);
        templateG2D.setColor(Color.decode("#FFFFFF"));
        templateG2D.drawString(drawName, x, y);
        return this;
    }

    public File build() {
        templateG2D.dispose();
        templateImage.flush();
        File file = new File("./avatar/" + this.fullName.hashCode() + ".png");
        try {
            ImageIO.write(templateImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // System.gc();
        return file;
    }
}