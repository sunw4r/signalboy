package signalboy.custom;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImgUtils {

    public static Rectangle getBounds(BufferedImage img, Color fillColor) {
        int top = getYInset(img, 20, 0, 1, fillColor);
        int bottom = getYInset(img, 20, img.getHeight() - 1, -1, fillColor);
        int left = getXInset(img, 0, top, 1, fillColor);
        int right = getXInset(img, img.getWidth() - 1, top, -1, fillColor);

        return new Rectangle(left, top, right - left, bottom - top);
    }

    public static int getYInset(BufferedImage img, int x, int y, int step, Color fillColor) {
        while (new Color(img.getRGB(x, y), true).equals(fillColor)) {
            y += step;
        }
        return y;
    }

    public static int getXInset(BufferedImage img, int x, int y, int step, Color fillColor) {
        while (new Color(img.getRGB(x, y), true).equals(fillColor)) {
            x += step;
        }
        return x;
    }

}
