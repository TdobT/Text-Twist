package serverPackage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import commonPackage.UDP_words;

public class UDP_task implements Runnable{

	private DatagramSocket udpDataSocket;
	private UserContainer users;
	
	UDP_task(DatagramSocket udpDataSocket, UserContainer users) {
		this.udpDataSocket = udpDataSocket;
		this.users = users;
	}
	
	@Override
	public void run() {
		
		DatagramPacket dataPacketIn = null;
		byte[] incData = new byte[1000000];
		
		System.out.println("Server UDP pronto.");
		
		while (true) {
			
			try {
	
				dataPacketIn = new DatagramPacket(incData, incData.length);
				udpDataSocket.receive(dataPacketIn);
	
				ByteArrayInputStream in = new ByteArrayInputStream(dataPacketIn.getData());
				ObjectInputStream is = new ObjectInputStream(in);
				
				UDP_words words = (UDP_words) is.readObject();
				
				Game game = users.getGameByID(words.gameID);
				if (!game.sendWords(words.words, users.getUser(words.player))) 
					System.out.println("Errore: impossibile aggiungere le parole del giocatore");;
				
				
			} catch (IOException e) {
				System.out.println("Errore di comunicazione");
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				System.out.println("Errore nella lettura della classe delle parole");
				e.printStackTrace();
			}
		}
	}
}
