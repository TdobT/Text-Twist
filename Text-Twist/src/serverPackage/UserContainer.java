package serverPackage;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import commonPackage.Multicast_rankings;
import commonPackage.RankingItem;
import serverPackage.User.userState;

public class UserContainer {
	
	private LinkedBlockingQueue<User> users;
	private LinkedBlockingQueue<Game> gamesRequest;
	
	
	UserContainer() {
		users = new LinkedBlockingQueue<User>();
		gamesRequest = new LinkedBlockingQueue<Game>();
	}

	
	public synchronized boolean exist(String name) {

		for (User u: users) {

			if (u.getName().equals(name)) return true; 
		}
		return false;
	}
	
	
	public synchronized User getUser(String name) {
		
		for (User u:users) {
			if (u.getName().equals(name)) {
				return u;
			}
		}
		return null;
	}
	
	/*
	 * Adds a new User and set's a new sessionID.
	 * This number is used to be sure of the Client identity
	 * and to not let any other user to do action for him
	 * after he logged in. Theres a small chance that 2 users
	 * have the same sessionID, but this doens't preclude the
	 * success of the methods wich use it.
	 */
	public synchronized boolean addUser(User user) { 
		
		if (!exist(user.getName())) {
			users.add(user); 
			user.setSessionID((new Random()).nextInt(10000000));
			return true;
		}
		return false;
	}
	
	
	/*
	 * returns the game if it succeed, every user exist and is online
	 * returns null otherwise
	 */
	public synchronized Game addGame(ArrayList<String> usersSend, String creator) {

		for (String u: usersSend) {
			if (!exist(u)) return null;
		}
		
		LinkedBlockingQueue<User> gameUsers = new LinkedBlockingQueue<User>();
		
		for (String u:usersSend) {
			User el = getUser(u);
			if (!el.getState().equals(userState.ONLINE)) { return null;}
			gameUsers.add(el);
		}
		
		Game game = new Game(gameUsers, creator);
		this.gamesRequest.add(game);
		
		return game;
	}
	
	public synchronized boolean removeGame(Game game) {
		return this.users.remove(game);
	}
	 
	public synchronized Game getGameByID(int gameID) {
		
		for (Game g: gamesRequest) {
			if (g.gameID == gameID) return g;
		}
		return null;
	}
	
	public synchronized String[] getUsersName() {
		String[] userNames = new String[users.size()];
		
		int c = 0;
		for (User us: users) {
			userNames[c] = us.getName();
			c++;
		}
		return userNames;
	}
	
	public synchronized Multicast_rankings getGlobalRanking() {
		
		ArrayList<RankingItem> ranks = new ArrayList<RankingItem>();
		for (User u: users) ranks.add(new RankingItem(u.getName(), u.getPoints()));
		Multicast_rankings rankings = new Multicast_rankings(ranks);
		rankings.orderRanks();
		
		return rankings;
	}
	
	public synchronized ArrayList<String> getOnlineUsers() {
		ArrayList<String> userOnline = new ArrayList<String>();

		for (User u: users) if (u.getState().equals(userState.ONLINE)) userOnline.add(u.getName());
		return userOnline;
	}
	
	
}
