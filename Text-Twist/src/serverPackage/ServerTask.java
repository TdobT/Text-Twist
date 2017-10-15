package serverPackage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;


import commonPackage.Multicast_rankings;
import serverPackage.User.userState;

public class ServerTask implements Runnable {

	private Socket socket;
	private UserContainer users;
	private Multicast_task multicastTask;
	private ServerState serverState;
	
	ServerTask(Socket socket, UserContainer users, Multicast_task multicast, ServerState serverState) {
		this.socket = socket;
		this.users = users;
		this.multicastTask = multicast;
		this.serverState = serverState;
	}
	
	
	@Override
	public void run() {
		String playerName = null;
		String creator = null;
		ObjectOutputStream writer = null;
		ObjectInputStream reader = null;
		
		try {
			
			System.out.println("Thread del server esegue un task di un client");
			writer = new ObjectOutputStream (socket.getOutputStream());
			reader = new ObjectInputStream (socket.getInputStream());

			int actionType = reader.readInt();

			Game game = null;
			
			/*
			 * 0 for answer to: a game request (creating a game):
			 * 	 in a game request, it must first send the total number of invited players,
			 * 	 and after that his name, and all the names of the other players.
			 * 
			 * 
			 * 1 for answer to: a game invite:
			 * 	 after a game request, other players choose to accept or refuse the invite;
			 *   if they refuse, the server must warn all the other players.
			 *   
			 *   
			 * 2 for answer to: a ranking list request
			 * 
			 */
			switch(actionType) {
			
			case 0:
				
				createGame(writer, reader, creator, game);
				
				break;
			
			case 1:

				acceptGame(writer, reader, playerName, game);
				
				break;
			
			case 2: 
				
				sendRanking(writer, reader);
				
				break;
				
			default:
				closeTCP(writer, reader, socket);
			
			}
		} catch (IOException e) {
			
			System.out.println("Errore di comunicazione col client. Chiusura forzata.");
			
		} finally {
			
			try {
				if (writer != null) writer.close();
				if (reader != null) reader.close();
			} catch (IOException e) {
			}
		}
	}
	
	private void createGame(
			ObjectOutputStream writer, 
			ObjectInputStream reader, 
			String creator, 
			Game game) {

		try {
			// reads the number of player other than the creator
			int playerNumber = reader.readInt();
			ArrayList<String> players = new ArrayList<String>();

			// reads the name of the creator of the game
			creator = reader.readUTF();
			players.add(creator);

			int sessionID = users.getUser(creator).getSessionID();
			if (sessionID != reader.readInt() || sessionID == 0) {
				// User failed to autenticate
				closeTCP(writer, reader, socket);
				return;
			}
			
			for (int i = 0; i < playerNumber; i++) {
				players.add(reader.readUTF());
			}

			game = users.addGame(players, creator);
			if (game == null) {
				// somethings bad happened: some user doesn't exist, or isn't online.
				writer.writeBoolean(false);
				writer.flush();
				closeTCP(writer, reader, socket);
				return;
			} 

			writer.writeBoolean(true);
			writer.writeInt(game.gameID);
			writer.writeInt(multicastTask.getMulticastPort());
			writer.flush();

			// if it arrives at this point, every user requested exist, and is online.
			// theyr data is in the game object, wich is saved inside the userContainer users.
			for (User u: game.getGameUsers()) {
				try {
					System.out.println("Invio chiamate ai giocatori");
					u.getCallback().gameCall(
							creator, 
							game.gameID, 
							this.multicastTask.getMulticastPort());
				} catch (RemoteException e) {
					System.out.println(
							"Errore: alcuni utenti non possono ricevere una richiesta di partita");
				}
			}

			// Closing TCP connection
			closeTCP(writer, reader, socket);

			boolean readyGame = false;
			// sleeps until the game is ready (everyone accepted) or 7 minutes passed
			for (int i = 0; i < 7 * 60; i++) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					System.out.println("Attesa interrotta inaspettatamente");
					e.printStackTrace();
				}
				if (game.readyGame()) {
					readyGame = true;
					break;
				}
				if (game.isClosing()) {
					break;
				}
			}

			try {
				if (readyGame) { play(game); }
			} catch (InterruptedException e) {
				System.out.println("Attesa interrotta inaspettatamente");
				e.printStackTrace();
			}
			// at this point everyone sent his possible permutation or the timeout has been reached,
			// so the server calculate the scores and close the game
			if (game.calculateRanking() == false) {
				System.out.println("Fatal Error: Cannot calculate ranking.");
				game.close();
				return;
			}
			
			int[] points = game.getRanking();
			LinkedBlockingQueue<User> userQueue = game.getGameUsers();

			// Updates ranking and server state file
			int c = 0;
			for (User u: userQueue) {
				u.addPoints(points[c]);
				serverState.updateUser(u);
				c++;
			}
			
			// sends result to all players with multicast
			multicastTask.sendMulticastRanking(points, userQueue);
			
			// delete game
			users.removeGame(game);
		
		} catch (IOException e1) {
			if (creator != null) users.getUser(creator).setState(userState.OFFLINE);
			System.out.println("Errore di comunicazione col server.");
		}
		
	}
	
	private void acceptGame(
			ObjectOutputStream writer,
			ObjectInputStream reader,
			String playerName,
			Game game) {

		try {
			// reads the name of the accepting / refusing player, and the gameID
			playerName = reader.readUTF();
			
			int sessionID = users.getUser(playerName).getSessionID();
			if (reader.readInt() != sessionID || sessionID == 0) {
				// User failed to autenticate.
				game.close();
				closeTCP(writer, reader, socket);
				return;
			}
			
			int gameID = reader.readInt();

			// reads the answer: true for accept, false for refuse
			boolean answer = reader.readBoolean();

			// reads all the gameID of the games he must refuse to accept this one
			int refNumber = reader.readInt();

			// closes this games
			for (int i = 0; i < refNumber; i++) users.getGameByID(reader.readInt()).close();


			game = users.getGameByID(gameID);
			if (game == null) {
				// if game is null, it doesnt exist (it could be already closed)
				// sends an error to the player and breaks (error coded with -1)

				writer.writeInt(-1);
				writer.flush();
				closeTCP(writer, reader, socket);
				return;
			}

			User player = users.getUser(playerName);
			if (player == null) {
				// this user has invalid name, or went offline.
				// reports an error with TCP connection and close.
				// (error coded with -2)

				writer.writeInt(-2);
				writer.flush();
				game.close();
				closeTCP(writer, reader, socket);
				return;
			}

			if (!answer) {
				// if the player refuses, the server must close the game and warn all the player invited.
				// here it closes the game, so every other thread can detect the incongruence and warn the players.
				// (coded with -3)

				writer.writeInt(-3);
				writer.flush();
				game.close();
				closeTCP(writer, reader, socket);
				return;
			}

			// evetything went fine
			writer.writeInt(0);
			writer.flush();


			if (!game.acceptPlayer(player)) {
				// this user already has accepted this game
				System.out.println("Inconsistenza nell'accettazione della partita");
			}

			/* 
			 * since it has accepted, this thread must wait with the connection open;
			 * it will wait until 7 total minute passed (the main thread wich created the
			 * game is aware of it and will close the game in the eventuality) or until
			 * some other thread close it since some player had refused the invite.
			 */
			socket.setSoTimeout(1000);

			for (int i = 0; i < 60 * 7; i++) {

				try {
					// useless read just to wait the right time and
					// to see if the client is still connected
					reader.readObject();
					
				} catch (SocketTimeoutException e) {
					
					if (game.isClosing()) {
						
						// game closed for timeout or for a refused invite
						// the client waiting for game words see the connection
						// get closed and conclude the game has been closed.
						System.out.println("closing " + game);
						closeTCP(writer, reader, socket);
						return;
					} 
					
					if (game.isStarted()) break;
					
				} catch (IOException e) {

					System.out.println("Persa la connessione col client, chisura della partita");
					game.close();
					closeTCP(writer, reader, socket);
					return;

				} catch (ClassNotFoundException e) {
				} 
			}

			if (!socket.isClosed() && game.isStarted()) {
				
				writer.writeUTF(game.getGameWord());
				writer.flush();
			}
			
		} catch (IOException e) {

			if (playerName != null) users.getUser(playerName).setState(userState.OFFLINE);
		}
	}
	
	private void sendRanking(
			ObjectOutputStream writer,
			ObjectInputStream reader) {
		
		// Reuse of MUL_rank class
		Multicast_rankings rankings = users.getGlobalRanking();
		
		try {
			writer.writeObject(rankings);
			writer.flush();
		} catch (IOException e) {
			System.out.println("Impossibile inviare la classifica");
		}
	
		closeTCP(writer, reader, socket);
		
	}
	
	private void closeTCP(ObjectOutputStream o, ObjectInputStream i, Socket s) {

		try  {
			o.close();
			i.close();
			s.close();

		} catch (IOException e ) {
			System.out.println("Impossibile chiudere la connessione o il socket." );
		}
	}
	
	private void play(Game game) throws IOException, InterruptedException{
		
		HashSet<String> wordsSet = Game.words;
		
		if (wordsSet == null) {
			System.out.println("Fatal Error: non existent words file. Cannot choose the words for the game");
			game.close();
			return;
		}
		
		Random random = new Random();
		String word;
		
		// find a String in the file wich is at least 6 character long
		while (true) {
			word = "";
			int pos = random.nextInt(wordsSet.size());
			for (String el: wordsSet) {
				if (pos == 0) {
					word = el;
					break;
				}
				pos--;
			}
			if (word.length() > 6) break;
		}
		
		// shuffle the string
		ArrayList<String> s = new ArrayList<String>();
		for (int j = 0; j < word.length(); j++) {s.add(word.substring(j, j+1));}
		java.util.Collections.shuffle(s);
		String shuffledWord = "";
		for (String st: s) { shuffledWord += st;}
		
		game.setGameWord(shuffledWord);
		game.start();
		
		for (int j = 0; j < 60 * 5; j++) {
			Thread.sleep(1000);
			if (game.everyOnePlayed()) break;
		}

	}
}
