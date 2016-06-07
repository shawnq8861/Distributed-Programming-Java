package Mobile;

import java.io.*;
import java.net.*;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;

/**
 * Mobile.Place is the our mobile-agent execution platform that accepts an
 * agent transferred by Mobile.Agent.hop( ), deserializes it, and resumes it
 * as an independent thread.
 *
 * @author  Munehiro Fukuda
 * @version %I% %G$
 * @since   1.0
 */
public class Place extends UnicastRemoteObject implements PlaceInterface {
    private AgentLoader loader = null;  // a loader to define a new agent class
    private int agentSequencer = 0;     // a sequencer to give a unique agentId
    private static int ipBase = 0;		// the base number for the sequencer
    private String message = null;		// message received from remote agent

    /**
     * This constructor instantiates a Mobile.AgentLoader object that
     * is used to define a new agent class coming from remotely.
     */
    public Place( ) throws RemoteException {
    	super( );
    	loader = new AgentLoader( );
    }

    /**
     * deserialize( ) deserializes a given byte array into a new agent.
     *
     * @param buf a byte array to be deserialized into a new Agent object.
     * @return a deserialized Agent object
     */
    private Agent deserialize( byte[] buf ) 
	throws IOException, ClassNotFoundException {
    	// converts buf into an input stream
        ByteArrayInputStream in = new ByteArrayInputStream( buf );

        // AgentInputStream identify a new agent class and deserialize
        // a ByteArrayInputStream into a new object
        AgentInputStream input = new AgentInputStream( in, loader );
        return ( Agent )input.readObject();
    }

    /**
     * transfer( ) accepts an incoming agent and launches it as an independent
     * thread.
     *
     * @param classname The class name of an agent to be transferred.
     * @param bytecode  The byte code of  an agent to be transferred.
     * @param entity    The serialized object of an agent to be transferred.
     * @return true if an agent was accepted in success, otherwise false.
     */
    public boolean transfer( String classname, byte[] bytecode, byte[] entity )
    		throws RemoteException {
    	//
    	// Implement by yourself.
    	//
    	// (1) Register this calling agent classname and bytecode into 
    	// AgentLoader.
    	// (2) Deserialize this agent entity through deserialize( entity ).
    	// (3) Set this agent identifier if it has not yet been set. How to
    	// give a new agent id is up to your implementation. An example is to 
    	// have each Place maintain a sequencer, to generate a unique agent
    	// id with a combination of the Place IP address and this sequence 
    	// number, and increment the sequencer.
    	// (4) Instantiate a Thread object by passing the deserialized agent 
    	// to the Thread constructor.
    	// (5) Invoke this thread start( ) method.
    	// (6) Return true if everything is done in success, otherwise false.
    	//
    	
    	// Register this calling agent classname and bytecode into 
    	// AgentLoader.
    	Class agentClass = loader.loadClass(classname, bytecode);
    	// Deserialize this agent entity through deserialize( entity ).
    	Agent agent = null;
 
    	try {
			agent = deserialize(entity);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
    	
    	// Set this agent identifier if it has not yet been set.
    	agent.setId(agentSequencer + ipBase);
    	++agentSequencer;
    	
    	// Instantiate a Thread object by passing the deserialized agent 
    	// to the Thread constructor.
    	Thread agentThread = new Thread(agent, "agent");
    	
    	// Invoke this thread start( ) method.
    	agentThread.start();
    	// Return true if everything is done in success, otherwise false.
    	return true;
    }
  
    /**
     * writeMessage( ) accepts an incoming message and writes it to a place
     * instance variable.
     *
     * @param message A String that holds a message received from an agent.
     * @return none.
     */
    public synchronized void writeMessage( String message ) 
    		throws RemoteException {
    	this.message = message;
    }

    /**
     * readMessage( ) reads a message from a place instance variable and 
     * returns it.
     *
     * @param none.
     * @return message A String that holds a message to be sent to an agent.
     */
    public synchronized String readMessage( ) 
    		throws RemoteException {
    	return this.message;
    }
    
    /**
     * main( ) starts an RMI registry in local, instantiates a Mobile.Place
     * agent execution platform, and registers it into the registry.
     *
     * @param args receives a port, (i.e., 5001-65535).
     */
    public static void main( String args[] ) {
    	//
    	// Implement by yourself.
    	//
    	// (1) Read args[0] as the port number and checks its validity.
    	// (2) Invoke startRegistry( int port ).
    	// (3) Instantiate a Place object.
    	// (4) Register it into rmiregistry through Naming.rebind( ).
    	//
    	
    	// Read args[0] as the port number and checks its validity.
    	int port = Integer.parseInt(args[0]);
    	if ( port < 5000 || port > 65535 ) {
    		System.err.println( 
    		"Usage: java -cp Mobile.jar Mobile.Place port (<6555, >=5000)" );
    		System.exit( -1 );
    	}
    	// Invoke startRegistry( int port ).
    	else {
    		try {
				startRegistry( port );
			} catch (RemoteException e) {
				e.printStackTrace();
			}
    	}
    	// Instantiate a Place object.
    	Place place = null;
    	try {
			place = new Place();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    	// Register it into rmiregistry through Naming.rebind( ).
    	try {
			Naming.rebind( "rmi://localhost:" + port + "/place", place );
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
    	
    	// initialize the base value of the agentSequencer
    	InetAddress ip = null;
    	try {
			ip = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
    	String ipString = ip.getHostAddress();
    	String[] ipStrArr = ipString.split("\\.");
    	for (int i = 0; i < ipStrArr.length; ++i) {
    		// shift each octet and add it to the total
    		ipBase += (Integer.valueOf(ipStrArr[i]) << ((3 - i) * 8));
    	}
    	System.out.println("ipBase: " + ipBase);
    }
    
    /**
     * startRegistry( ) starts an RMI registry process in local to this Place.
     * 
     * @param port the port to which this RMI should listen.
     */
    private static void startRegistry( int port ) throws RemoteException {
        try {
            Registry registry =
                LocateRegistry.getRegistry( port );
            registry.list( );
        }
        catch ( RemoteException e ) {
            Registry registry =
                LocateRegistry.createRegistry( port );
        }
    }
}