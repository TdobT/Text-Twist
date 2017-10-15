package serverPackage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import commonPackage.Multicast_rankings;
import commonPackage.RankingItem;

public class Multicast_task {

	
	DatagramSocket dataSocket;
	InetAddress multicastAddress;
	int multicastPort;
	
	Multicast_task(DatagramSocket dataSocket, InetAddress multicastAddress, int multicastPort) {
		this.multicastAddress = multicastAddress;
		this.dataSocket = dataSocket;
		this.multicastPort = multicastPort;
	}
	
	
	public void sendMulticastRanking(int[] userPoints, LinkedBlockingQueue<User> players) {

		int cont = 0;
		byte[] bytes = null;
		Multicast_rankings rankings;
		ArrayList<RankingItem> ranks = new ArrayList<RankingItem>();
		try {
			
			// Prepares to send a "Multicast_rankings" object to multicast channel
			for (User u: players) {
				RankingItem usRank = new RankingItem(u.getName(), userPoints[cont]);
				ranks.add(usRank);
				cont++;
			}
			rankings = new Multicast_rankings(ranks);
			rankings.orderRanks();
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream daos = new ObjectOutputStream(baos);
			daos.writeObject(rankings);
			daos.close();
			bytes = baos.toByteArray();
			
		} catch (IOException e) {
			System.out.println("Impossibile preparare il buffer per l'invio dei dati in multicast..." );
			return;
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
