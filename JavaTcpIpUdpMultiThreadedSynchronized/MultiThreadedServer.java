import java.net.*;
import java.io.*;
import java.util.*;

class MultiThreadedServer implements Runnable {
    public static void main( String args[] ) {
	int port = Integer.parseInt( args[0] ); // arg[0]: port 
	try {
	    ServerSocket server = new ServerSocket( port );
	    while ( true ) {
		Socket socket = server.accept( );
		Thread child =  new Thread( new MultiThreadedServer( socket ) );
		child.start( );
	    }
	} catch ( IOException e ) { }
    }

    private Socket socket = null;
    private Scanner input = null;
    public MultiThreadedServer( Socket socket ) {
	this.socket = socket;
	try {
	    input = new Scanner( socket.getInputStream( ) );
	} catch ( IOException e ) { }
    }
    public void run( ) {
	while ( input.hasNext( ) ) {
	    System.out.println( input.next( ) );
	}
	try {
	    socket.close( );
	} catch ( IOException e ) { }
    }
}
