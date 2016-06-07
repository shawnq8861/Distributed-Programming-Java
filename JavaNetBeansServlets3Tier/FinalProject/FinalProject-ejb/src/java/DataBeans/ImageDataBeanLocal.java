/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DataBeans;

import javax.ejb.Local;
import java.awt.image.BufferedImage;

/**
 *
 * @author Shawn Quinn
 */
@Local
public interface ImageDataBeanLocal {

    BufferedImage getImageDataFromSource(String imageName);

    BufferedImage getCurrentImage();

    void saveImage(BufferedImage bi, String imageName);
    
}
