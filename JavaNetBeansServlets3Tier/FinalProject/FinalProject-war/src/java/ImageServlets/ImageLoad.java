 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ImageServlets;

import DataBeans.ImageDataBeanLocal;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 *
 * @author Shawn
 */
public class ImageLoad extends HttpServlet {

    ImageDataBeanLocal imageDataBean = lookupImageDataBeanLocal();
    
    private String imageKey = null;
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // set configuration of HTTP response to display an image
        response.setContentType("image/jpeg");
        // get the image from the session bean
        BufferedImage bi = imageDataBean.getImageDataFromSource(imageKey);
        // get the output stream
        OutputStream out = response.getOutputStream();
        // write the image to the output stream
	ImageIO.write(bi, "jpg", out);
	out.close();
        
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // get the key for the selected image
        imageKey = request.getParameter("image");
        
        // redirect the response
        RequestDispatcher view =
            request.getRequestDispatcher("selectResponse.jsp");
        view.forward(request, response);
   
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    private ImageDataBeanLocal lookupImageDataBeanLocal() {
        try {
            Context c = new InitialContext();
            return (ImageDataBeanLocal) c.lookup("java:global/FinalProject/FinalProject-ejb/ImageDataBean!DataBeans.ImageDataBeanLocal");
        } catch (NamingException ne) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }

}
