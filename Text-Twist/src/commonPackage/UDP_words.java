package commonPackage;

import java.io.Serializable;

public class UDP_words implements Serializable{

	private static final long serialVersionUID = 6317533870617383938L;
	
	public String[] words;
	public int gameID;
	public String player;
	
	public UDP_words(String[] words, int gameID, String player) {
		
		this.words = words;
		this.gameID = gameID;
		this.player = player;
	}

}
