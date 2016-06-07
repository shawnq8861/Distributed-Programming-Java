/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DataBeans;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Shawn Quinn
 */
@Stateful
public class ImageDataBean implements ImageDataBeanLocal {
    private BufferedImage buffImage = null;
    private static String currentImage = null;

    @PersistenceContext(unitName = "FinalProject-ejbPU")
    private EntityManager em;

    public void persist(Object object) {
        em.persist(object);
    }

    @Override
    public BufferedImage getImageDataFromSource(String imageName) {
        currentImage = imageName;
        String sql = "SELECT imageUrl FROM imageurls i WHERE i.imageName = \"";
        sql = sql.concat(imageName);
        sql = sql.concat("\"");
        String path = String.valueOf(em.createNativeQuery(sql).getSingleResult());
        File file = new File(path);
        try {
            buffImage = ImageIO.read(file);
        } catch(IOException ioe) {
            }
        
        return buffImage;
    }

    @Override
    public BufferedImage getCurrentImage() {
        String sql = "SELECT imageUrl FROM imageurls i WHERE i.imageName = \"";
        sql = sql.concat(currentImage);
        sql = sql.concat("\"");
        String path = String.valueOf(em.createNativeQuery(sql).getSingleResult());
        File file = new File(path);
        try {
            buffImage = ImageIO.read(file);
        } catch(IOException ioe) {
            }
        
        return buffImage;
    }

    @Override
    public void saveImage(BufferedImage bi, String imageName) {
        String sql = "SELECT imageUrl FROM imageurls i WHERE i.imageName = \"";
        sql = sql.concat(imageName);
        sql = sql.concat("\"");
        String path = String.valueOf(em.createNativeQuery(sql).getSingleResult());
        //File outputFile = new File(url);
        File outputFile = new File(path);
        try {
            ImageIO.write(bi, "jpg", outputFile);
        } catch (IOException ex) {
            Logger.getLogger(ImageDataBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
