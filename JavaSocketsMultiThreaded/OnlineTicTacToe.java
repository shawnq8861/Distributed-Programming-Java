
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.BindException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
*
* @author Munehiro Fukuda
* modified on October 25, 2015 by
* @author Shawn Quinn
*
*/
public class OnlineTicTacToe implements ActionListener {
	
	private final int INTERVAL = 1000; // 1 second
	private final int NBUTTONS = 9; // number of buttons
	private ObjectInputStream input = null; // input from my counterpart
	private ObjectOutputStream output = null; // output from my counterpart
	private JFrame window = null; // the tic-tac-toe window
	private JButton[] button = new JButton[NBUTTONS]; // button[0] - button[9]
	private boolean[] myTurn = new boolean[1]; // T: my turn, F: your turn
	private String myMark = null; // "O" or "X"
	private String yourMark = null; // "X" or "O"
	private ServerSocket server = null;	// server will be player "X"
	private Socket client = null;		// client will be player "O"
	private boolean isServer = false;	// true for server, false for client

	/**
	 * Prints out the usage.
	 */
	private static void usage( ) {
		System.err.println( "Usage: java OnlineTicTacToe ipAddr ipPort(>=5000)" );
		System.exit( -1 );
	}
	/**
	 * Prints out the track trace upon a given error and quits the application.
	 * @param an exception
	 */
	private static void error( Exception e ) {
		e.printStackTrace();
		System.exit(-1);
	}

	/**
	 * Starts the online tic-tac-toe game.
	 * @param args[0]: my counterpart's ip address, args[0]: his/her port
	 */
	public static void main( String[] args ) {
		// verify the number of arguments
		if ( args.length != 2 ) {
			usage( );
		}

		// verify the correctness of my counterpart address
		InetAddress addr = null;
		try {
			addr = InetAddress.getByName( args[0] );
		} catch ( UnknownHostException e ) {
			error( e );
		}

		// verify the correctness of my counterpart port
		int port = 0;
		try {
			port = Integer.parseInt( args[1] );
		} catch (NumberFormatException e) {
			error( e );
		}
		if ( port < 5000 ) {
			usage( );
		}

		// now start the application
		OnlineTicTacToe game = new OnlineTicTacToe( addr, port );
	}

	/**
	 * Is the constructor that sets up a TCP connection with my counterpart,
	 * brings up a game window, and starts a slave thread for listening to
	 * my counterpart.
	 * @param my counterpart's ip address
	 * @param my counterpart's port
	 */
	public OnlineTicTacToe( InetAddress addr, int port ) {
		//
		// set up a TCP connection with my counterpart
		//
		// Use non-blocking accept and BindException handling if running on
		// a single machine, i.e. localhost
		//
		// Use non-blocking accept and allow connecting as client or server
		// if running on 2 separate machines
		//
		String hostName = addr.getHostName();
		System.out.println("hostname = " + hostName);
		//
		// use non-blocking accept and handle bind exception if using localhost
		//
		if (hostName.equals("localhost")) {
			System.out.println("attempting to bind to localhost...");
			try {
				server = new ServerSocket( port );
				// Set the server non-blocking, (i.e. time out beyond 1000)
				server.setSoTimeout(INTERVAL);
				isServer = true;	// localhost available
				System.out.println( "server created..." );
			} catch ( BindException e ) {
				// port not available, will need to connect as a client
				isServer = false;	// localhost in use
			} 	catch ( IOException ioe ) {
					error( ioe );
				}
			while (true) {
				if (isServer) {
					try {
						// Try to accept a connection as a server
						System.out.println( "trying to accept as server..." );
						client = server.accept();
						myTurn[0] = false;
					} catch ( SocketTimeoutException ste ) {
						// Couldn't receive a connection request within INTERVAL
					} 	catch ( IOException ioe ) {
							error( ioe );
						}
					if ( client != null ) {
						System.out.println( "I have accepted..." );
						myTurn[0] = false;
						break;
					}
				}
				else {
					try {
						// try to request a connection as a client
						System.out.println( "trying to connect as client..." );
						client = new Socket(addr, port);
						myTurn[0] = true;
					} catch ( IOException ioe ) {
						// Connection refused
						error( ioe );
					}
					if ( client != null ) {
						System.out.println( "I have connected..." );
						myTurn[0] = true;
						break;
					}
				}
			}
		}
		//
		//	allow connecting as either server or client if not using localhost
		//
		else {
			try {
				System.out.println( "trying to create a server..." );
				server = new ServerSocket( port );
				// Set the server non-blocking, (i.e. time out beyond 1000)
				server.setSoTimeout(INTERVAL);
			} catch ( Exception e ) {
				error( e );
			}
	
			// While accepting a remote request, try to send my connection request
	
			while ( true ) {
				try {
					// Try to accept a connection as a server
					System.out.println( "trying to accept as server..." );
					client = server.accept();
				} catch ( SocketTimeoutException ste ) {
					// Couldn't receive a connection request within INTERVAL
				} 	catch ( IOException ioe ) {
			    		error( ioe );
		    		}
				// Check if a connection was established. If so, leave the loop
				if ( client != null ) {
					System.out.println( "I have accepted..." );
					myTurn[0] = false;
					isServer = true;
					break;
				}
				// If accept() times out, try to request a connection as a client
				try {
					System.out.println( "trying to connect as client..." );
					client = new Socket(addr, port);
				} catch ( IOException ioe ) {
						// Connection refused
				}
		    	// Check if a connection was established, If so, leave the loop
				if ( client != null ) {
					System.out.println( "I have connected..." );
					myTurn[0] = true;
					isServer = false;
					break;
				}
			}
		}
		System.out.println( "TCP connection established..." );
		// instantiate output stream to allow the event handler to send
		// messages to the other player
		try {
			output = new ObjectOutputStream(client.getOutputStream());
		} catch (Exception e) {
			error( e );
		}
	
		// set up a window
		makeWindow( myTurn[0] ); // either makeWindow(true) or makeWindow( false );
		// start my counterpart thread
		Counterpart counterpart = new Counterpart( );
		counterpart.start();
	}

	/**
	 * Creates a 3x3 window for the tic-tac-toe game
	 * @param true if this window is created by the 1st player, false by
	 * the 2nd player
	 */
	private void makeWindow( boolean amFormer ) {
		System.out.println( "Setting up game window..." );
		myTurn[0] = amFormer;
		myMark = ( amFormer ) ? "O" : "X"; // 1st person uses "O"
		yourMark = ( amFormer ) ? "X" : "O"; // 2nd person uses "X"
		// create a window
		window = new JFrame("OnlineTicTacToe(" +
				((amFormer) ? "former)" : "latter)" ) + myMark );
  
		window.setSize(350, 350);
    
		// add code to reposition the window after setting the size
		// use the value of amFormer to set the offset point values
		if (amFormer) {
			window.setLocation(0, 100);		// O's window
		}
		else {
			window.setLocation(500, 100);	// X's window   	
		}
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setLayout(new GridLayout(3, 3));
		// initialize all nine cells.
		for (int i = 0; i < NBUTTONS; i++) {
			button[i] = new JButton();
			window.add(button[i]);
			button[i].addActionListener(this);
		}
		// make it visible
		window.setVisible(true);
	}

	/**
	 * Marks the i-th button with mark ("O" or "X")
	 * @param the i-th button
	 * @param a mark ( "O" or "X" )
	 * @param true if it has been marked in success
	 */
	private boolean markButton( int i, String mark ) {
		if ( button[i].getText( ).equals( "" ) ) {
			button[i].setText( mark );
			button[i].setEnabled( false );
			return true;
		}
		return false;
	}

	/**
	 * Checks which button has been clicked
	 * @param an event passed from AWT
	 * @return an integer (0 through to 8) that shows which button has been
	 * clicked. -1 upon an error.
	 */
	private int whichButtonClicked( ActionEvent event ) {
		for ( int i = 0; i < NBUTTONS; i++ ) {
			if ( event.getSource( ) == button[i] )
				return i;
		}
		return -1;
	}

	/**
	 * Checks if the i-th button has been marked with mark( "O" or "X" ).
	 * @param the i-th button
	 * @param a mark ( "O" or "X" )
	 * @return true if the i-th button has been marked with mark.
	 */
	private boolean buttonMarkedWith( int i, String mark ) {
		return button[i].getText( ).equals( mark );
	}

	/**
	 * Checks if the player with mark( "O" or "X" ) has won the game.
	 * @param a mark ( "O" or "X" )
	 * @return true if 3 consecutive buttons in a row, column or diagonal 
	 * are marked for the mark of interest.
	 */
	private boolean isWinner( String mark ) {
		// check the rows
		for ( int i = 0; i < 7; i += 3) {
			if ( buttonMarkedWith( i, mark )   &&
				 buttonMarkedWith( i+1, mark ) &&
				 buttonMarkedWith( i+2, mark ) ) {
				return true;
			}
		}
		// check the columns
		for ( int i = 0; i < 3; i++ ) {
			if ( buttonMarkedWith( i, mark )   &&
				 buttonMarkedWith( i+3, mark ) &&
				 buttonMarkedWith( i+6, mark ) ) {
				return true;
			}
		}
		// check the diagonals
		if ( buttonMarkedWith( 0, mark ) &&
			 buttonMarkedWith( 4, mark ) &&
			 buttonMarkedWith( 8, mark ) ) {
			return true;
		}
		if ( buttonMarkedWith( 2, mark ) &&
			 buttonMarkedWith( 4, mark ) &&
			 buttonMarkedWith( 6, mark ) ) {
			return true;
		}
		return false;
	}
	
	/**
	 * Pops out another small window indicating that mark("O" or "X") won!
	 * @param a mark ( "O" or "X" )
	 */
	private void showWon( String mark ) {
		JOptionPane.showMessageDialog( null, mark + " won!" );
	}

	/**
	 * Is called by AWT whenever any button has been clicked. You have to:
	 * <ol>
	 * <li> check if it is my turn,
	 * 
	 * <li> check which button was clicked with whichButtonClicked( event ),
	 * 
	 * <li> mark the corresponding button with markButton( buttonId, mark ),
	 * 
	 * <li> send this information to my counterpart
	 * 
	 * <li> checks if the game was completed with
	 * buttonMarkedWith( buttonId, mark )
	 * 
	 * <li> shows a winning message with showWon( )
	 */
	public void actionPerformed( ActionEvent event ) {
		// check if it is my turn
		int buttonNumber = -1;
		System.out.println("entered event handler...");
		
		if (myTurn[0] == true) {
			System.out.println("my turn...");
			// grab the lock...
			synchronized ( myTurn ) {
				// get the button click	
				buttonNumber = whichButtonClicked(event);			
				String button = Integer.toString(buttonNumber);
				markButton(buttonNumber, myMark);
				System.out.println("clicked on button " + button);
				// send the button click to the other player
				try {
					output.writeObject(button);
				} catch (Exception e) {
					error( e );
				}
				if (isWinner(myMark)) {
					showWon(myMark);
				}
				// toggle myTurn to prevent button marking until the other player
				// completes their turn
				myTurn[0] = false;
				// release the lock...
				myTurn.notifyAll();
			}	
		}
		else {
			System.out.println("not my turn...");
		}		
	}

	/**
	 * This is a reader thread that keeps reading from and behaving as my
	 * counterpart.
	 */
	private class Counterpart extends Thread {
		/**
		 * Is the body of the Counterpart thread.
		 */
		
		// constructor
		public Counterpart() {
			// instantiate input stream to allow reading messages from the
			// other player
			try {
				input = new ObjectInputStream(client.getInputStream());
			} catch (Exception e) {
				error( e );
			}
		}
		@Override
		public void run( ) {
			System.out.println("counterpart thread started,...");
			System.out.println("isServer = " + isServer);
			// listen for messages from the other player
			int length = 0;			// length of message String
			char lastChar = ' ';	// last character in message String
			int buttonNumber = -1;	// button number passed in message
			String message = "";
			while(true) {
				// try to grab the lock...
				synchronized ( myTurn ) {
					while ( myTurn[0] == true ) {
			    		try{
			    			//set the current thread to wait
			    			System.out.println("waiting on lock...");
			    			myTurn.wait();
			    		}catch(InterruptedException ex){
			    			//someone wake me up.
			    		}
			    	}
					// now have the lock...
					System.out.println("waiting for my counterpart...");
					try {
						message = (String)input.readObject();
					} catch(Exception e) {
						error (e);
					}
					length = message.length();
					// parse the button number character off end of message
					lastChar = message.charAt(length - 1);
					System.out.println("counterpart clicked button " + lastChar);
					// convert button number char to int
					buttonNumber = Character.getNumericValue(lastChar);
					markButton(buttonNumber, yourMark);
					// check to see if game is over
					if (isWinner(yourMark)) {
						showWon(yourMark);
					}
					// toggle myTurn to allow marking a button
					myTurn[0] = true;
					// release the lock...
					myTurn.notifyAll();
				}
			}			
		}
	}
}
