package JSpace;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * JSpace.JSpace is the JSpace server program of our JavaSpace implementation, 
 * (i.e., JSpace). It provides constructors for creation of master and 
 * distributed slave JSpace servers.  It also processes user JSpace requests 
 * through UDP multicast and JSpace write(), read(), and take().
 *
 * @author  Shawn Quinn
 * @date	12/27/2015
  */

public class JSpace {
	private final int READ = 0;
	private final int WRITE = 1;
    private final int TAKE = 2;
    private InetAddress group = null;
    private MulticastSocket mSocket = null; // a socket to multicast UDP packet
    private int port = 0;
    private int numServers = 0;
    private int serverID = -1;
    Hashtable<String, Entry> byteTable = null;
    Hashtable<String, Entry> charTable = null;
    Hashtable<String, Entry> shortTable = null;
    Hashtable<String, Entry> intTable = null;
    Hashtable<String, Entry> longTable = null;
    Hashtable<String, Entry> floatTable = null;
    Hashtable<String, Entry> doubleTable = null;
    Hashtable<String, Entry> stringTable = null;

    /**
     * The constructor for the master JSpace server. 
     *
     * The constructor receives a connection array, group IP address and a 
     * port to multicast a UDP messages to JSpace clients.
     *
     * @param multicast_group a group IP address that should be in a range of
     *        "224.0.0.0" - "239.255.255.255".
     * @param port a port that should be in a range of 5001 - 65535.
     *
     * Upon a slave server initialization, the master should send the 
     * following information to the slave:
     * (1) Multicast group IP     * (2) Port     * (3) # of servers (including the master), say N     * (4) Server ID (where the master receives 0 and slaves are identified 
     * with 1 through to N - 1)
     *
     * Waits for "show" and quit messages from the keyboard
     */
    public JSpace( Connection[] connections, String multicast_group, 
    		int port ) {
    	
    	// the main body of the master server
  
    	// set master iD = 0
    	serverID = 0;
    	// set number of servers = number of connections + 1
    	numServers = connections.length + 1;
    	// send UDP info to other servers using connections
    	try {
    		String messageBase = multicast_group;
    		messageBase = messageBase.concat(":");
    		messageBase = messageBase.concat(String.valueOf(port));
    		messageBase = messageBase.concat(":");
    		messageBase = messageBase.concat(String.valueOf(numServers));
    		messageBase = messageBase.concat(":");
    		for ( int i = 0; i < connections.length; i++ ) {
    			String messageTo = messageBase;
    			// append server ID
    			messageTo = messageTo.concat(String.valueOf(i + 1));
    			connections[i].out.writeObject(messageTo);
    			connections[i].out.flush();
    		}
    	} catch ( Exception e ) {
    		e.printStackTrace( );
    		System.exit( -1 );
    	}
    	    	
    	// wait for command line input
    	String commandIn = "";
    	boolean keepReadingStdin = true;
    	while (keepReadingStdin) {
    		System.out.print("command: ");
    		commandIn = System.console().readLine();
    		if (commandIn.equals("show")) {
    			// send “show” to slave servers
    			for ( int i = 0; i < connections.length; i++ ) {
        			try {
						connections[i].out.writeObject(commandIn);
						connections[i].out.flush();
						String message = (String)connections[i].in.readObject();
						System.out.println(message);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
        		}    			
    		}
    		else if (commandIn.equals("quit")) {
    			// send "quit" to slave servers
    			for ( int i = 0; i < connections.length; i++ ) {
        			try {
						connections[i].out.writeObject(commandIn);
						connections[i].out.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
        		}
    			keepReadingStdin = false;   
    		}
    	}
    	// close the ssh2 connection with each remote machines
    	for ( int i = 0; i < connections.length; i++ ) {
    		connections[i].close( );
    	}
    	
    	System.exit(0);
    }

    /**
     * The constructor for the slave JSpace server. 
     *
     * The constructor receives a connection
     *
     * Waits for "show" and "quit messages from the server
     */
    public JSpace( Connection connection ) {
    	
    	// the main body of the slave server

    	// main thread reads UDP info from master server
    	String messageFrom = "";
    	try {
    		messageFrom = (String)connection.in.readObject();
    	} catch(IOException ioe) {
    	} catch (ClassNotFoundException e) {
		}
    	// parse to message for UDP parameters...
    	String[] splitMessage = messageFrom.split(":");
    	String multicast_group = splitMessage[0];
    	try {
			group = InetAddress.getByName( multicast_group );
		} catch (UnknownHostException e) {
		}
    	String portString = splitMessage[1];
    	port = Integer.valueOf(portString);
    	String numServStr = splitMessage[2];
    	numServers = Integer.valueOf(numServStr);
    	String idString = splitMessage[3];
    	serverID = Integer.valueOf(idString);
    	    	
        byteTable = new Hashtable<String, Entry>();
        charTable = new Hashtable<String, Entry>();
        shortTable = new Hashtable<String, Entry>();
        intTable = new Hashtable<String, Entry>();
        longTable = new Hashtable<String, Entry>();
        floatTable = new Hashtable<String, Entry>();
        doubleTable = new Hashtable<String, Entry>();
        stringTable = new Hashtable<String, Entry>();
    	
    	// then main thread launches UDP listen thread
    	JSpaceRequestReceiver requestReceiver = new JSpaceRequestReceiver();
    	requestReceiver.start();

    	// main thread then listens for messages from admin "show" or "quit"
    	boolean continueListen = true;
    	while (continueListen) {
    		try {
				messageFrom = (String)connection.in.readObject();
			} catch (ClassNotFoundException e) {
			} catch (IOException e) {
			}
    		if (messageFrom.equals("show")) {
    			// send hash table contents to master
    			String message = "slave-";
    			message = message.concat(String.valueOf(serverID));
    			message = message.concat(buildMessage());
    			try {
					connection.out.writeObject(message);
				} catch (IOException e) {
					}
    		}
    		else if (messageFrom.equals("quit")) {
    			continueListen = false;
    		}
    	}
    	// close the connection when finished
    	connection.close();
    	// close the receiver thread
    	requestReceiver.interrupt();
    	
    	System.exit(0);
    }
    
    /*
     * map servers to types based on number of slave servers and server id
     * use alternating algorithm:
     * 		1 slave server:  1,1,1,1,1...
     * 		2 slave servers: 1,2,1,2,1...
     * 		3 slave servers: 1,2,3,1,2...
     * 		4 slave servers: 1,2,3,4,1...
     * 
     * using the following type order:
     * 		byte, char, short, int, long, float, double, string
     * 
     * example, 2 slave servers:
     * 		server 1 has byte, short, long, double
     * 		server 2 has char, int, float, string
     * 
     */
    private boolean isHashed(int numSlaves, int serverId, String varType) {
    	boolean hashed = false;
    	
    	// case 1:  type == byte
    	if (varType.equals("byte") && (serverId == 1)) {
    		hashed = true;
    	}
    	// case 2:  type == char
    	else if (varType.equals("char")) {
    		if (serverId == 1 && numSlaves == 1) {
    			hashed = true;
    		}
    		else if (serverId == 2) {
    			hashed = true;
    		}
    		else {
    			hashed = false;
    		}
    	}
    	// case 3:  type == short
    	else if (varType.equals("short")) {
    		if (serverId == 1 && (numSlaves == 1 || numSlaves == 2)) {
    			hashed = true;
    		}
    		else if (serverId == 3) {
    			hashed = true;
    		}
    		else {
    			hashed = false;
    		}
    	}
    	// case 4:  type == int
    	else if (varType.equals("int")) {
       		if (serverId == 1 && (numSlaves == 1 || numSlaves == 3)) {
    			hashed = true;
    		}
    		else if (serverId == 2 && numSlaves == 2) {
    			hashed = true;
    		}
    		else if (serverId == 4) {
    			hashed = true;
    		}
    		else {
    			hashed = false;
    		}
    	}
    	// case 5:  type == long
    	else if (varType.equals("long")) {
       		if (serverId == 1 && (numSlaves == 1 || numSlaves == 2 || 
       				numSlaves == 4)) {
    			hashed = true;
    		}
    		else if (serverId == 2 && numSlaves == 3) {
    			hashed = true;
    		}
    		else {
    			hashed = false;
    		}
    	}
    	// case 6:  type == float
    	else if (varType.equals("float")) {
    		if (serverId == 1 && numSlaves == 1) {
    			hashed = true;
    		}
    		else if (serverId == 2 && (numSlaves == 2 || numSlaves == 4)) {
    			hashed = true;
    		}
    		else if (serverId == 3 && numSlaves == 3) {
    			hashed = true;
    		}
    		else {
    			hashed = false;
    		}
    	}
    	// case 7:  type == double
       	else if (varType.equals("double")) {
      		if (serverId == 1 && (numSlaves == 1 || numSlaves == 2 || 
      				numSlaves == 3)) {
    			hashed = true;
    		}
    		else if (serverId == 3 && numSlaves == 4) {
    			hashed = true;
    		}
    		else {
    			hashed = false;
    		}
       	}
    	// case 8:  type == string
    	else if (varType.equals("string")) {
    		if (serverId == 1 && numSlaves == 1) {
    			hashed = true;
    		}
    		else if (serverId == 2 && (numSlaves == 2 || numSlaves == 3)) {
    			hashed = true;
    		}
    		else if (serverId == 4) {
    			hashed = true;
    		}
    		else {
    			hashed = false;
    		}
    	}   	
    	return hashed;
    }
 
    /*
     * construct return string to send after receiving "show" command
     * 
     */
	private String buildMessage() {
		String message = "";
		int count = 0;
		int numSlaves = numServers - 1;
		if (isHashed(numSlaves, serverID, "byte")) {
    		Enumeration<String> enumKeys = byteTable.keys();
    		while(enumKeys.hasMoreElements()) {
    			String key = enumKeys.nextElement();
    		    Entry entryVal = byteTable.get(key);
    		    message = message.concat("\n       ");
    		    message = message.concat(String.valueOf(count));
				message = message.concat(": byte         variable       ");
				message = message.concat(String.valueOf(entryVal.getValue()));
				++count;
    		}
    	}
		if (isHashed(numSlaves, serverID, "char")) {
    		Enumeration<String> enumKeys = charTable.keys();
    		while(enumKeys.hasMoreElements()) {
    			String key = enumKeys.nextElement();
    		    Entry entryVal = charTable.get(key);
    		    message = message.concat("\n       ");
    		    message = message.concat(String.valueOf(count));
				message = message.concat(": char         variable       ");
				message = message.concat(String.valueOf(entryVal.getValue()));
				++count;
    		}
    	}  		
		if (isHashed(numSlaves, serverID, "short")) {
    		Enumeration<String> enumKeys = shortTable.keys();
    		while(enumKeys.hasMoreElements()) {
    			String key = enumKeys.nextElement();
    		    Entry entryVal = shortTable.get(key);
    		    message = message.concat("\n       ");
    		    message = message.concat(String.valueOf(count));
				message = message.concat(": short        variable       ");
				message = message.concat(String.valueOf(entryVal.getValue()));
				++count;
    		}
    	}
		if (isHashed(numSlaves, serverID, "int")) {
    		Enumeration<String> enumKeys = intTable.keys();
    		while(enumKeys.hasMoreElements()) {
    			String key = enumKeys.nextElement();
    		    Entry entryVal = intTable.get(key);
    		    message = message.concat("\n       ");
    		    message = message.concat(String.valueOf(count));
				message = message.concat(": int          variable       ");
				message = message.concat(String.valueOf(entryVal.getValue()));
				++count;
    		}
    	}
		if (isHashed(numSlaves, serverID, "long")) {
    		Enumeration<String> enumKeys = longTable.keys();
    		while(enumKeys.hasMoreElements()) {
    			String key = enumKeys.nextElement();
    		    Entry entryVal = longTable.get(key);
    		    message = message.concat("\n       ");
    		    message = message.concat(String.valueOf(count));
				message = message.concat(": long         variable       ");
				message = message.concat(String.valueOf(entryVal.getValue()));
				++count;
    		}
    	}
		if (isHashed(numSlaves, serverID, "float")) {
    		Enumeration<String> enumKeys = floatTable.keys();
    		while(enumKeys.hasMoreElements()) {
    			String key = enumKeys.nextElement();
    		    Entry entryVal = floatTable.get(key);
    		    message = message.concat("\n       ");
    		    message = message.concat(String.valueOf(count));
				message = message.concat(": float        variable       ");
				message = message.concat(String.valueOf(entryVal.getValue()));
				++count;
    		}
    	}
		if (isHashed(numSlaves, serverID, "double")) {
    		Enumeration<String> enumKeys = doubleTable.keys();
    		while(enumKeys.hasMoreElements()) {
    			String key = enumKeys.nextElement();
    		    Entry entryVal = doubleTable.get(key);
    		    message = message.concat("\n       ");
    		    message = message.concat(String.valueOf(count));
				message = message.concat(": double       variable       ");
				message = message.concat(String.valueOf(entryVal.getValue()));
				++count;
    		}
    	}
		if (isHashed(numSlaves, serverID, "string")) {
    		Enumeration<String> enumKeys = stringTable.keys();
    		while(enumKeys.hasMoreElements()) {
    			String key = enumKeys.nextElement();
    		    Entry entryVal = stringTable.get(key);
    		    message = message.concat("\n       ");
    		    message = message.concat(String.valueOf(count));
				message = message.concat(": string       variable       ");
				message = message.concat(String.valueOf(entryVal.getValue()));
				++count;
    		}
    	}
		return message;
	}

    private class JSpaceRequestReceiver extends Thread {
    	
    	private Object lock = (Object)0;
    	
    	public JSpaceRequestReceiver() {
    		try {
    			mSocket = new MulticastSocket( port );
    			mSocket.joinGroup( group );
    		} catch( IOException e ) {
    			}
    	}
    		
    	@Override
    	public void run() {
    		// always listening for UDP messages, unless interrupted by parent
    		DatagramPacket packet = null;
    		while (!Thread.interrupted()) {
    			try {
    				byte[] buf = new byte[256];
    				packet = new DatagramPacket( buf, buf.length );
    				mSocket.receive( packet ); // wait for request from client
    			} catch (Exception e) {
    				}
    			// start a messageHandler thread to process request
    			MessageHandler handler = new MessageHandler(packet, lock);
    			handler.start();
    		}	
    	}
    }
    
    private class MessageHandler extends Thread {
    	private DatagramSocket dSocket = null;  // a socket to send a UDP packet
    	private DatagramPacket receivedPacket;
    	private Object lock;
    	
    	public MessageHandler(DatagramPacket packet, Object lock) {
    		try {
				dSocket = new DatagramSocket();
			} catch (SocketException e) {
				}
    		receivedPacket = packet;
    		this.lock = lock;
    	}
 
    	@Override
    	public void run() {
    		Entry receivedEntry = null;
			try {
				receivedEntry = Entry.deserialize(receivedPacket.getData());
			} catch (ClassNotFoundException | IOException e1) {
				} 
    		int numSlaves = numServers - 1;
    		//
    		// Process write() requests...
    		//
    	    if (receivedEntry.getOperation() == WRITE) {
    	    	// add entry value to appropriate hash table
    	    	if (isHashed(numSlaves, serverID, "byte") && 
    	    			(receivedEntry.getType().equals("byte"))) {
    	    		synchronized(lock) {
    	    			byteTable.put(receivedEntry.getName(), receivedEntry);
    	    			lock.notifyAll();
    	    		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "char") && 
    	    			(receivedEntry.getType().equals("char"))) {
    	    		synchronized(lock) {
    	    			charTable.put(receivedEntry.getName(), receivedEntry);
    	    			lock.notifyAll();
    	    		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "short") && 
    	    			(receivedEntry.getType().equals("short"))) {
    	    		synchronized(lock) {
    	    			shortTable.put(receivedEntry.getName(), receivedEntry);
    	    			lock.notifyAll();
    	    		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "int") && 
    	    			(receivedEntry.getType().equals("int"))) {
    	    		synchronized(lock) {
    	    			intTable.put(receivedEntry.getName(), receivedEntry);
    	    			lock.notifyAll();
    	    		}
    	    		
            	}
    	    	else if (isHashed(numSlaves, serverID, "long") && 
    	    			(receivedEntry.getType().equals("long"))) {
    	    		synchronized(lock) {
    	    			longTable.put(receivedEntry.getName(), receivedEntry);
    	    			lock.notifyAll();
    	    		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "float") && 
    	    			(receivedEntry.getType().equals("float"))) {
    	    		synchronized(lock) {
    	    			floatTable.put(receivedEntry.getName(), receivedEntry);
    	    			lock.notifyAll();
    	    		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "double") && 
    	    			(receivedEntry.getType().equals("double"))) {
    	    		synchronized(lock) {
    	    			doubleTable.put(receivedEntry.getName(), receivedEntry);
    	    			lock.notifyAll();
    	    		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "string") && 
    	    			(receivedEntry.getType().equals("string"))) {
    	    		synchronized(lock) {
    	    			stringTable.put(receivedEntry.getName(), receivedEntry);
    	    			lock.notifyAll();
    	    		}
            	}
    	    }
    	    //
    	    // process read() requests...
    	    //
    	    else if (receivedEntry.getOperation() == READ) {
    	    	Entry returnEntry = null;
    	    	// attempt to read entry value from appropriate hash table
    	    	if (isHashed(numSlaves, serverID, "byte") && 
    	    			(receivedEntry.getType().equals("byte"))) {
            		synchronized (lock) {
            			while (!byteTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = byteTable.get(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "char") && 
    	    			(receivedEntry.getType().equals("char"))) {
            		synchronized (lock) {
            			while (!charTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = charTable.get(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "short") && 
    	    			(receivedEntry.getType().equals("short"))) {
            		synchronized (lock) {
            			while (!shortTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = shortTable.get(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "int") && 
    	    			(receivedEntry.getType().equals("int"))) {
            		synchronized (lock) {
            			while (!intTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = intTable.get(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "long") && 
    	    			(receivedEntry.getType().equals("long"))) {
            		synchronized (lock) {
            			while (!longTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = longTable.get(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "float") && 
    	    			(receivedEntry.getType().equals("float"))) {
            		synchronized (lock) {
            			while (!floatTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = floatTable.get(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "double") && 
    	    			(receivedEntry.getType().equals("double"))) {
            		synchronized (lock) {
            			while (!doubleTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = doubleTable.get(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "string") && 
    	    			(receivedEntry.getType().equals("string"))) {
            		synchronized (lock) {
            			while (!stringTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = stringTable.get(receivedEntry.getName());
            		}
            	}
            	if (returnEntry != null) {   	
            		byte[] buf = new byte[256];
            		// serialize
            		try {
            			buf = Entry.serialize(returnEntry);
            		} catch(Exception ioe) {	
    	    			}
            		// create the packet
            		DatagramPacket returnPacket = new DatagramPacket(buf, buf.length, 
            				receivedPacket.getAddress(), port);
            		// send to client
            		try {
            			dSocket.send(returnPacket);
            		} catch(Exception ioe) {
    	    			}
            	}	
    	    }
    	    //
    	    // process take() requests...
    	    //
    	    else if (receivedEntry.getOperation() == TAKE) {
    	    	Entry returnEntry = null;
    	    	// attempt to take entry value from appropriate hash table   	    	
    	    	if (isHashed(numSlaves, serverID, "byte") && 
    	    			(receivedEntry.getType().equals("byte"))) {
            		synchronized (lock) {
            			while (!byteTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = byteTable.remove(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "char") && 
    	    			(receivedEntry.getType().equals("char"))) {
            		synchronized (lock) {
            			while (!charTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = charTable.remove(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "short") && 
    	    			(receivedEntry.getType().equals("short"))) {
            		synchronized (lock) {
            			while (!shortTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = shortTable.remove(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "int") && 
    	    			(receivedEntry.getType().equals("int"))) {
            		synchronized (lock) {
            			while (!intTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = intTable.remove(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "long") && 
    	    			(receivedEntry.getType().equals("long"))) {
            		synchronized (lock) {
            			while (!longTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = longTable.remove(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "float") && 
    	    			(receivedEntry.getType().equals("float"))) {
            		synchronized (lock) {
            			while (!floatTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = floatTable.remove(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "double") && 
    	    			(receivedEntry.getType().equals("double"))) {
            		synchronized (lock) {
            			while (!doubleTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = doubleTable.remove(receivedEntry.getName());
            		}
            	}
    	    	else if (isHashed(numSlaves, serverID, "string") && 
    	    			(receivedEntry.getType().equals("string"))) {	
            		synchronized (lock) {
            			while (!stringTable.containsKey(receivedEntry.getName())) {
            				try {
								lock.wait();
							} catch (InterruptedException e) {
								}
            			}
            			returnEntry = stringTable.remove(receivedEntry.getName());
            		}
            	}
            	if (returnEntry != null) {   	
            		byte[] buf = new byte[256];
            		// serialize
            		try {
            			buf = Entry.serialize(returnEntry);
            		} catch(Exception ioe) {	
    	    			}
            		// create the packet
            		DatagramPacket returnPacket = new DatagramPacket(buf, buf.length, 
            				receivedPacket.getAddress(), port);
            		// send to client
            		try {
            			dSocket.send(returnPacket);
            		} catch(Exception ioe) {
    	    			}
            	}  	
    	    }
    	}
    }
}
