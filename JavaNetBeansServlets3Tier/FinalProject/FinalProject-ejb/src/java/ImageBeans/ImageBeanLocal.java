/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ImageBeans;

import javax.ejb.Local;
import java.awt.image.BufferedImage;

/**
 *
 * @author Shawn Quinn
 */
@Local
public interface ImageBeanLocal {

    BufferedImage convertToGrayscale(BufferedImage bi);

    BufferedImage threshold(BufferedImage bi);

    BufferedImage overlay(BufferedImage bi);
    
}
