/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ImageBeans;

import javax.ejb.Stateless;
import java.awt.image.BufferedImage;

/**
 *
 * @author Shawn Quinn
 */
@Stateless
public class ImageBean implements ImageBeanLocal {

    private static BufferedImage buffImage = null;
    private static BufferedImage modImage = null;
    private static final int THRESHOLD = 127;
    private static final int OVER_RED = 0;
    private static final int OVER_GREEN = 0;
    private static final int OVER_BLUE = 255;

    public ImageBean() {

    }
    
    @Override
    public BufferedImage convertToGrayscale(BufferedImage bi) {
        
        modImage = new BufferedImage(bi.getColorModel(),
                                     bi.getRaster(),
                                     bi.isAlphaPremultiplied(),
                                     null);
        
        int h = modImage.getHeight();
        int w = modImage.getWidth();
        int colorValue = 0;
        int grayValue = 0;
        int red = 0;
        int green = 0;
        int blue = 0;
        double gray = 0;
        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                // read the color values for the current pixel
                colorValue = modImage.getRGB(x, y);
                red = (colorValue >> 16) & 0xFF;
                green = (colorValue >> 8) & 0xFF;
                blue = colorValue & 0xFF;
                gray = (.3 * red) + (.6 * green) + (.1 * blue);
                grayValue = 0;
                grayValue |= (int)gray;
                grayValue |= ((int)gray << 8);
                grayValue |= ((int)gray << 16);
                // set the color values of pixel to grayscale representation
                modImage.setRGB(x, y, grayValue);
            }
        }

        return modImage;
    } 

    @Override
    public BufferedImage threshold(BufferedImage bi) {

        modImage = new BufferedImage(bi.getColorModel(),
                                     bi.getRaster(),
                                     bi.isAlphaPremultiplied(),
                                     null);
        
        int h = modImage.getHeight();
        int w = modImage.getWidth();
        int colorValue = 0;
        int grayValue = 0;
        int red = 0;
        int green = 0;
        int blue = 0;
        double gray = 0;
        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                // read the color values for the current pixel
                colorValue = modImage.getRGB(x, y);
                red = (colorValue >> 16) & 0xFF;
                green = (colorValue >> 8) & 0xFF;
                blue = colorValue & 0xFF;
                gray = (.3 * red) + (.6 * green) + (.1 * blue);
                if (gray > THRESHOLD) {
                    gray = 255; // white
                }
                else {
                    gray = 0;   // black
                }
                grayValue = 0;
                grayValue |= (int)gray;
                grayValue |= ((int)gray << 8);
                grayValue |= ((int)gray << 16);
                // set the color values of pixel to grayscale representation
                modImage.setRGB(x, y, grayValue);
            }
        }

        return modImage;
    }

    @Override
    public BufferedImage overlay(BufferedImage bi) {

        modImage = new BufferedImage(bi.getColorModel(),
                                     bi.getRaster(),
                                     bi.isAlphaPremultiplied(),
                                     null);
        
        int h = modImage.getHeight();
        int w = modImage.getWidth();
        int colorValue = 0;
        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                // read the color values for the current pixel
                colorValue = modImage.getRGB(x, y);
                if ((x == w/2) || (y == h/2)) {
                    colorValue = 0;
                    colorValue |= OVER_BLUE;
                    colorValue |= (OVER_GREEN << 8);
                    colorValue |= (OVER_RED << 16);
                }
                // set the color values of pixel to grayscale representation
                modImage.setRGB(x, y, colorValue);
            }
        }
 
        return modImage;
    }
}
