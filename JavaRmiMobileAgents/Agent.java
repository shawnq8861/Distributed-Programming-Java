package Mobile;
import java.io.*;
import java.rmi.*;
import java.util.Arrays;
import java.lang.reflect.*;
import java.net.MalformedURLException;

/**
 * Mobile.Agent is the base class of all user-define mobile agents. It carries
 * an agent identifier, the next host IP and port, the name of the function to
 * invoke at the next host, arguments passed to this function, its class name,
 * and its byte code. It runs as an independent thread that invokes a given
 * function upon migrating the next host.
 *
 * @author  Munehiro Fukuda
 * @version %I% %G%
 * @since   1.0
 */
public class Agent implements Serializable, Runnable {
    // live data to carry with the agent upon a migration
    protected int agentId        = -1;    // this agent's identifier
    private String _hostname     = null;  // the next host name to migrate
    private String _function     = null;  // the function to invoke upon a move
    private int _port            = 0;     // the next host port to migrate
    private String[] _arguments  = null;  // arguments pass to _function
    private String _classname    = null;  // this agent's class name
    private byte[] _bytecode     = null;  // this agent's byte code

    /**
     * setPort( ) sets a port that is used to contact a remote Mobile.Place.
     * 
     * @param port a port to be set.
     */
    public void setPort( int port ) {
    	this._port = port;
    }

    /**
     * setId( ) sets this agent identifier: agentId.
     *
     * @param id an identifier to set to this agent.
     */
    public void setId( int id ) {
    	this.agentId = id;
    }

    /**
     * getId( ) returns this agent identifier: agentId.
     *
     * @param this agent's identifier.
     */
    public int getId( ) {
    	return agentId;
    }

    /**
     * getByteCode( ) reads a byte code from the file whose name is given in
     * "classname.class".
     *
     * @param classname the name of a class to read from local disk.
     * @return a byte code of a given class.
     */
    public static byte[] getByteCode( String classname ) {
    	// create the file name
    	String filename = classname + ".class";

    	// allocate the buffer to read this agent's bytecode in
    	File file = new File( filename );
    	byte[] bytecode = new byte[( int )file.length( )];

    	// read this agent's bytecode from the file.
    	try {
    		BufferedInputStream bis =
    				new BufferedInputStream( new FileInputStream( filename ) );
    		bis.read( bytecode, 0, bytecode.length );
    		bis.close( );
    	} catch ( Exception e ) {
    		e.printStackTrace( );
    		return null;
    	}

    	// now you got a byte code from the file. just return it.
    	return bytecode;	
    }

    /**
     * getByteCode( ) reads this agent's byte code from the corresponding file.
     *
     * @return a byte code of this agent.
     */
    public byte[] getByteCode( ) {
    	if ( _bytecode != null ) // bytecode has been already read from a file
    		return _bytecode; 
	
			// obtain this agent's class name and file name
		_classname = this.getClass( ).getName( );
			_bytecode = getByteCode( _classname );

		return _bytecode;
    }

    /**
     * run( ) is the body of Mobile.Agent that is executed upon an injection
     * or a migration as an independent thread. run( ) identifies the method 
     * with a given function name and arguments and invokes it. The invoked
     * method may include hop( ) that transfers this agent to a remote host or
     * simply returns back to run( ) that terminates the agent.
     */
    public void run( ) {
    	//
    	// Implement by yourself.
    	//
    	// (1) Find the method to invoke, through this.getClass( ).getMethod( ).
    	// (2) Invoke this method through Method.invoke( ).

    	// Find the method to invoke, through this.getClass( ).getMethod( ).
    	Method method = null;
    	try {
	    	if (this._arguments == null) {
	    		method = this.getClass().getMethod(this._function, (Class<?>[])null);
	    	}
	    	else {
	    		method = this.getClass().getMethod(this._function, this._arguments.getClass());
	    	}
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
    	
    	// Invoke this method through Method.invoke( ).  	
    	try {
    		if (this._arguments == null) {
	    		method.invoke(this, (Object[])null);
	    	}
	    	else {
	    		// When using reflection, type checking only occurs at runtime,
	    		// so there is no opportunity for the compiler to box the value
	    		// hence, an exact type match must be created for method params
	    		Object[] params = new Object[]{_arguments};
	    		method.invoke(this, params);
	    	}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// method.invoke may cause an exception, because it receives                                                                       
	        // a Thread.stop( ) signal.                                                                                                        
	        Writer result = new StringWriter( );
	        PrintWriter printWriter = new PrintWriter( result );
	        e.printStackTrace( printWriter );
	        if ( !result.toString( ).contains( "Thread.stop" ) )
	            e.printStackTrace( );
		}
    }

    /**
     * hop( ) transfers this agent to a given host, and invokes a given
     * function of this agent.
     *
     * @param hostname the IP name of the next host machine to migrate
     * @param function the name of a function to invoke upon a migration
     */    
    public void hop( String hostname, String function ) {
    	hop( hostname, function, null );
    }

    /**
     * hop( ) transfers this agent to a given host, and invokes a given
     * function of this agent as passing given arguments to it.
     *
     * @param hostname the IP name of the next host machine to migrate
     * @param function the name of a function to invoke upon a migration
     * @param args     the arguments passed to a function called upon a 
     *                 migration.
     */
    @SuppressWarnings( "deprecation" )
    public void hop( String hostname, String function, String[] args ) {
    	//
    	// Implement by yourself.
    	//
    	// (1) Load this agent byte code into the memory.
    	// (2) Serialize this agent into a byte array.
    	// (3) Find a remote place through Naming.lookup( ).
    	// (4) Invoke an RMI call.
    	// (5) Kill this agent with Thread.currentThread( ).stop( ), 
    	// which is deprecated but do so anyway.
    	//
 
    	// set the next destination to migrate to
    	_hostname = hostname;
 
    	// set the method to invoke at the next host
    	_function = function;
  
    	// get the arguments
    	_arguments = args;
    	
    	// get the class name
    	_classname = this.getClass().getName();
    	
    	// Load this agent byte code into the memory.
    	_bytecode = this.getByteCode(  );
    	
    	// Serialize this agent into a byte array.
    	byte[] entity = this.serialize();
    	
    	// Find a remote place through Naming.lookup( ).
    	PlaceInterface nextPlace = null;
    	try {
			nextPlace = 
		  (PlaceInterface)Naming.lookup
		  ( "rmi://" + _hostname + ":" + _port + "/place" );
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
    	// Invoke an RMI call, i.e. call Place.transfer().
    	try {
			nextPlace.transfer(_classname, _bytecode, entity);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    	// Kill this agent with Thread.currentThread( ).stop( )
    	Thread.currentThread().stop();
    }

    
    /**
     * send( ) transfers a message String to a given host.
     *
     * @param hostname the IP name of the host machine to migrate
     * @param message the String to send.
     * @return none.
     */
    public void send( String hostname, String message ) {
    	
    	// Find a remote place through Naming.lookup( ).
    	PlaceInterface sendPlace = null;
    	try {
			sendPlace = 
		  (PlaceInterface)Naming.lookup
		  ( "rmi://" + hostname + ":" + _port + "/place" );
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
    	// Invoke an RMI call, i.e. call Place.writeMessage().
    	try {
			sendPlace.writeMessage(message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * receive( ) transfers a message String from a given host.
     *
     * @param hostname the IP name of the host machine to migrate
     * @param message the String to send.
     * @return none.
     */
    public String receive( String hostname ) {
    	
    	// Find a remote place through Naming.lookup( ).
    	PlaceInterface sendPlace = null;
    	try {
			sendPlace = 
		  (PlaceInterface)Naming.lookup
		  ( "rmi://" + hostname + ":" + _port + "/place" );
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
    	// Invoke an RMI call, i.e. call Place.readMessage().
    	String message = null;
    	try {
			message = sendPlace.readMessage( );
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    	return message;
    }
    
    /**
     * serialize( ) serializes this agent into a byte array.
     *
     * @return a byte array to contain this serialized agent.
     */
    private byte[] serialize( ) {
    	try {
    		// instantiate an object output stream.
    		ByteArrayOutputStream out = new ByteArrayOutputStream( );
    		ObjectOutputStream os = new ObjectOutputStream( out );
	    
    		// write myself to this object output stream
    		os.writeObject( this );

    		return out.toByteArray( ); // convert the stream to a byte array
    	} catch ( IOException e ) {
    		e.printStackTrace( );
    		return null;
    	}
    }
}