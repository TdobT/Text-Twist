package serverPackage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.LinkedBlockingQueue;

public class MulticastTask {

	
	DatagramSocket dataSocket;
	InetAddress multicastAddress;
	int multicastPort;
	
	MulticastTask(DatagramSocket dataSocket, InetAddress multicastAddress, int multicastPort) {
		this.multicastAddress = multicastAddress;
		this.dataSocket = dataSocket;
		this.multicastPort = multicastPort;
	}
	
	
	public synchronized void sendMulticastRanking(int[] rankings, LinkedBlockingQueue<User> players) {

		int cont = 0;
		byte[] bytes = null;
		try {
			for (User u: players) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        DataOutputStream daos = new DataOutputStream(baos);
		        daos.writeUTF(u.getName());
		        daos.writeInt(rankings[cont]);
		        daos.close();
		        bytes = baos.toByteArray();
				cont++;
			}
		} catch (IOException e) {
			System.out.println("Impossibile preparare il buffer per l'invio dei dati in multicast..." );
		}
		 
		if (bytes == null) {
			System.out.println("Errore nella creazione del buffer");
			return;
		}
		
		DatagramPacket dataPacket = new DatagramPacket(bytes, bytes.length, multicastAddress, multicastPort);
		
		System.out.println("Invio dati sul Multicast");
		try {
			this.dataSocket.send(dataPacket);
		} catch (IOException e) {
			System.out.println("Impossibile inviare il messaggio multicast contenente la classifica a tutti gli utenti");
			e.printStackTrace();
		}
		
		
	}
	
	public int getMulticastPort() { return this.multicastPort; }
	
}
