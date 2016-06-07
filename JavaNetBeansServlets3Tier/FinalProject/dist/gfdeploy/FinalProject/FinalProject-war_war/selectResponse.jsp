<%-- 
    Document   : selectResponse
    Created on : Dec 14, 2015, 4:01:32 PM
    Author     : Shawn
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Image Loaded</title>
</head>
    <body>
        <h1 align="center" >Distributed Image Processor</h1>
        <form method="POST" action="ImageModify">
        <center>
            <select name="Image Processing Operations" size="1">
                <option value="ConvertToGrayscale"> Convert To Grayscale </option>
                <option value="Threshold"> Threshold </option>
                <option value="Overlay"> Overlay </option>
            </select>
        </center>
        <center>   
            <input type="Submit" value="Apply Operation">
        </center>
        The Image Requested
        <img src="ImageLoad" alt="Roxy" >
        </form>
    </body>
</html>
