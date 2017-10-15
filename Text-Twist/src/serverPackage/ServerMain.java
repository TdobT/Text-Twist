package serverPackage;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import commonPackage.RMI_server_interface;

import javax.xml.parsers.*;
import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerMain {

	public static void main(String[] args) {

		String configFileName = "ServerConfiguration.xml";
		if (args.length == 1) configFileName = args[0];
		else if (args.length > 1) {
			System.out.println("Devi inserire il nome del file di configurazione");
		}
		
		INIT_SERVER(configFileName);

	}
	
	
	@SuppressWarnings("resource")
	private static void INIT_SERVER(String configFileName) {

		Document doc = null;
		
		try {	
			
			File inputFile = new File(configFileName);
		    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		    doc = dBuilder.parse(inputFile);
		    doc.getDocumentElement().normalize();

		} catch (IOException e2) {
			System.out.println("Errore nell'apertura del file di configurazione. Impossibile continuare");
			e2.printStackTrace();
			System.exit(-1);
		} catch (ParserConfigurationException e2) {
			System.out.println("Errore nella conversione del file XML. Impossibile continuare");
			e2.printStackTrace();
			System.exit(-1);
		} catch (SAXException e2) {
			System.out.println("Errore nella lettura del file XML. Impossibile continuare");
			e2.printStackTrace();
			System.exit(-1);
		}
	
	    int serverPort = 0;
	    String serverName = null;
	    int registryPort = 0;
	    String wordsFileName = null;
	    int startingMulticastPort = 0;
	    int maxMulticastPort = 0;
	    int multicastPort = 0;
	    String multicastAddress = null;
	    int udpPort = 0;
	    DatagramSocket dataSocketUDP = null;
	    
	    String stateFileName = null;
	    
	    // Reading necessary items from configuration file
	    try {
	    	
	    	NodeList els = doc.getElementsByTagName("property");

	    	serverPort = Integer.parseInt(els.item(0).getFirstChild().getTextContent());
	    	serverName = els.item(1).getFirstChild().getTextContent();
	    	registryPort = Integer.parseInt(els.item(2).getFirstChild().getTextContent());
	    	wordsFileName = els.item(3).getFirstChild().getTextContent();
	    	startingMulticastPort = Integer.parseInt(els.item(4).getFirstChild().getTextContent());
	    	maxMulticastPort = Integer.parseInt(els.item(5).getFirstChild().getTextContent());
	    	multicastAddress = els.item(6).getFirstChild().getTextContent();
	    	udpPort = Integer.parseInt(els.item(7).getFirstChild().getTextContent());
	    	stateFileName = els.item(8).getFirstChild().getTextContent();
	    	
	    } catch (NumberFormatException e) {
	    	System.out.println("Errore nel file di configurazione. Impossibile continuare.");
	    	System.exit(-1);
	    }

	    multicastPort = startingMulticastPort;
	    
    	UserContainer users = new UserContainer();
    	
    	// trying to open and read the old state of the server
    	ServerState serverState = new ServerState(stateFileName, users);
    	serverState.recover();
    	
    	// Initializing data
	    RMI_server server = new RMI_server(serverName, serverPort, users, serverState);
	    
	    File words = new File(wordsFileName);
	    Game.createWordList(words);
	    
	    
	    // Searching for multicast address
    	InetAddress multAddress = null;
		try { multAddress = InetAddress.getByName(multicastAddress); }
		catch (UnknownHostException e2) {
			System.out.println("Impossibile determinare l'indirizzo dell'host " + multicastAddress);
		}
		
    	if (!multAddress.isMulticastAddress()) {
    		System.out.println("Questo indirizzo non è Multicast.. chiusura forzata.");
    		System.exit(-1);
    	}
    	
    	
    	// Creating datagram socket for multicast
    	DatagramSocket dataSocketMUL = null;
		try {
			dataSocketMUL = new DatagramSocket();
		} catch (SocketException e2) {
			System.out.println("Errors in the opening of a datagram socket.");
			e2.printStackTrace();
		}
	    
	    // Exporting RMI registry
		try{
			
			RMI_server_interface serverRMI = 
					(RMI_server_interface) UnicastRemoteObject.exportObject(server, 0);
			Registry registry = LocateRegistry.createRegistry(registryPort);
			registry.rebind(RMI_server_interface.OBJECT_NAME, serverRMI);
			
			System.out.println("Server RMI pronto.");
			
		} catch(RemoteException e){
			System.out.println("Server error:" + e.getMessage());
			System.exit(-1);
		}

		ThreadPoolExecutor thPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(100);
		
		
		
		
		// TASK ESECUZIONE RICHIESTE UDP
		try {
			dataSocketUDP = new DatagramSocket(udpPort);
		} catch (SocketException e) {
			System.out.println("Impossibile aprire un server UDP, chiusura forzata...");
			System.exit(-1);
		}
		thPool.execute(new UDP_task(dataSocketUDP, users));
		
		
		
		
		
		// TASK ESECUZIONE RICHIESTE TCP
		ServerSocket serverSocket = null;
		
		try {
			serverSocket = new ServerSocket(server.getPort());
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("Impossibile aprire un server Socket, chiusura forzata...");
			System.exit(-1);
			
		}
		System.out.println("Server TCP pronto.");
		
		while (true) {
			Socket socket;

			try {
				socket = serverSocket.accept();
				System.out.println("Client rilevato, thread attivato");
		    	Multicast_task multicast = 
		    			new Multicast_task(dataSocketMUL, multAddress, multicastPort);
		    	multicastPort++;
				thPool.execute(new ServerTask(socket, users, multicast, serverState));
				if (multicastPort > maxMulticastPort) multicastPort = startingMulticastPort;
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
