import java.net.*;
import java.io.*;
import java.util.*;

public class ClientSocket {
    public static void main( String args[] ) {
	String serverAddress = args[0];
	int serverIp = Integer.parseInt( args[1] );
	Scanner keyboard = new Scanner( System.in );
	try { 
	    Socket socket = new Socket( serverAddress, serverIp );
	    OutputStream output = socket.getOutputStream( );
	    while ( keyboard.hasNext( ) ) {
		String string = keyboard.next( );
		byte buf[] = string.getBytes( );
		output.write( buf );
	    }
	    socket.close( );
	} catch ( IOException e ) { }
    }
}
    
