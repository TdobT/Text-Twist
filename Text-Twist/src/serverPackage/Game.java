package serverPackage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class Game {

	public String creator;
	public int gameID;
	public static HashSet<String> words;
	
	private static int IDGenerator = 0;
	private LinkedBlockingQueue<User> gameRequestUsers;
	private LinkedBlockingQueue<Boolean> accepted;
	private String[][] playerWords;
	private boolean[] playerHasWords;
	private int[] rankingPoints;
	private int missingPlayers;
	private boolean closing = false;
	private boolean started = false;
	
	// it is assured it can't reach a deadlock because are almost
	// always not called in a nested lock
	private ReentrantLock closeLock = new ReentrantLock();
	private ReentrantLock startLock = new ReentrantLock();
	private ReentrantLock missingPlayerLock = new ReentrantLock();
	
	private String gameWord;
	
	public Game(LinkedBlockingQueue<User> gameRequestUsers, String creator) {
		
		this.gameRequestUsers = gameRequestUsers;
		this.creator = creator;
		accepted = new LinkedBlockingQueue<Boolean>();
		int dim = gameRequestUsers.size();
		playerWords = new String[dim][];
		playerHasWords = new boolean[dim];
		rankingPoints = new int[dim];
		
		for (int i = 0; i < dim; i++) {playerHasWords[i] = false; rankingPoints[i] = 0;}
		
		Iterator<User> it = gameRequestUsers.iterator();
		while (it.hasNext()) {
			accepted.add(false);
			it.next();
		}
		
		missingPlayers = dim;
		gameID = IDGenerator;
		IDGenerator++;
	}
	
	public synchronized LinkedBlockingQueue<User> acceptedQueue() { 
		
		LinkedBlockingQueue<User> res = new LinkedBlockingQueue<User>();

		Iterator<User> uItr = gameRequestUsers.iterator();
		Iterator<Boolean> bItr = accepted.iterator();
		
		while (uItr.hasNext()) {
			if (bItr.next()) res.add(uItr.next());
			else uItr.next();
		}
		
		return res;
	}
	
	public synchronized boolean acceptPlayer(User user) {

		closeLock.lock();
		if (closing) {
			closeLock.unlock();
			return false;
		}
		closeLock.unlock();
		
		Iterator<User> uItr = gameRequestUsers.iterator();
		Iterator<Boolean> bItr = accepted.iterator();
		
		while (uItr.hasNext()) {

			if (uItr.next() == user) {
				Boolean isAlreadyAccepted = bItr.next();
				if (isAlreadyAccepted) return false;
				
				isAlreadyAccepted = true;
				
				// with this, threads doesn't have to wait
				// when needs to see if the game started
				missingPlayerLock.lock();
				missingPlayers--;
				missingPlayerLock.unlock();
				return true;
			}
			bItr.next();
		}
		
		return false;
	}
	
	public synchronized boolean sendWords(String[] words, User player) {
		
		int playerPosition = 0;
		for (User u: gameRequestUsers) {
			if (u == player) {
				playerWords[playerPosition] = words;
				playerHasWords[playerPosition] = true;
				return true;
			}
			playerPosition++;
		}
		return false;
	}
	
	public synchronized String[] getPlayerWords(User player) {
		
		int playerPosition = 0;
		for (User u: gameRequestUsers) {
			if (u == player) {
				return playerWords[playerPosition];
			}
			playerPosition++;
		}
		return null;
		
	}
	
	public synchronized boolean everyOnePlayed() {
		for (int i = 0; i < playerHasWords.length; i++) {
			if (!playerHasWords[i]) return false;
		}
		return true;
	}
	
	public boolean calculateRanking() {
		
		if (words == null) return false;
		
		int currPlayer = 0;
		for (User u: gameRequestUsers) {
			if (playerWords[currPlayer] != null) {
				
				String[] remPW = removeDuplicates(playerWords[currPlayer]);
				for (int i = 0; i < remPW.length; i++) {
					
					String pW = remPW[i];
					if (pW == null) continue;
					if (isValid(pW, gameWord) && isInFile(pW)) {
						
						rankingPoints[currPlayer] += pW.length();
						u.addPoints(pW.length());
					}
				}
			}
			currPlayer++;
		}
		return true;
	}
	
	private String[] removeDuplicates(String[] word) {
		String[] remPW = new String[word.length];
		
		int remDimension = 0;
		
		for (int i = 0; i < word.length; i++) {
			if (word[i] == null) continue;
			boolean alreadyCont = false;
			int j ;
			for (j = 0; j < remDimension; j++) {
				if (remPW[j].equals(word[i])) { alreadyCont = true; break; }
			}
			if (!alreadyCont) {
				remPW[j] = word[i];
				remDimension++;
			}
		}
		
		return remPW;
	}
	
	private boolean isValid(String word, String base) {

		ArrayList<String> w = new ArrayList<String>(), b = new ArrayList<String>();
		for (int j = 0; j < word.length(); j++) w.add(word.substring(j, j+1)); 
		for (int j = 0; j < base.length(); j++) b.add(base.substring(j, j+1));
		
		// Strings implements comparable
		b.sort(null);
		w.sort(null);
		
		if (b.size() < w.size()) return false;
		
		/*
		 * the words single letters group must be a subset of the base single letter group so,
		 * when searching for a letter in a position i in the first array, it can already start
		 * at position i in the second array.
		 * 
		 */
		int j = 0, i = 0;
		
		while (i < w.size() && j < b.size()) {
			if (w.get(i).equals(b.get(j))) {
				i++;
				j++;
			} else j++;
		}
		return (i == w.size());
	}
	
	private boolean isInFile(String word) {
		return (words.contains(word));
	}
	
	public static void createWordList(File wordsFile){
		Game.words = new HashSet<String>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(wordsFile))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	
		    	Game.words.add(line);
		    }
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public int[] getRanking() { return this.rankingPoints; }
	
	public boolean isClosing() { return this.closing; }
	
	public void close() { this.closing = true; }
	
	public LinkedBlockingQueue<User> getGameUsers() { return this.gameRequestUsers; }
	
	public boolean readyGame() { 
		missingPlayerLock.lock();
		boolean mP = missingPlayers == 0;
		missingPlayerLock.unlock();
		return mP;
	}
	
	public void start() { startLock.lock(); this.started = true; startLock.unlock(); }
	
	public boolean isStarted() { 
		startLock.lock(); 
		Boolean s = this.started; 
		startLock.unlock();
		return s;
	}
	
	public void setGameWord(String word) { this.gameWord = word; }
	
	public String getGameWord() { return this.gameWord; }
	

}
