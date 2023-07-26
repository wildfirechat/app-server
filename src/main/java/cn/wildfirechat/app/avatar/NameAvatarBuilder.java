package cn.wildfirechat.app.avatar;

import java.awt.*;
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
        // Get the FontMetrics
        Font font = templateG2D.getFont().deriveFont(40f);
        FontMetrics metrics = templateG2D.getFontMetrics(font);
        // Determine the X coordinate for the text
        int x = (templateWidth - metrics.stringWidth(drawName)) / 2;
        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
        int y = ((templateHeight - metrics.getHeight()) / 2) + metrics.getAscent();
        // Set the font
        templateG2D.setFont(font);
        // Draw the String
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