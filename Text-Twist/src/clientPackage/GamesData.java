package clientPackage;

public class GamesData {
	
	private String creator;
	private int gameID;
	private int multicastGamePort;
	
	GamesData(String creator, int gameID, int multicastGamePort) {
		this.creator = creator;
		this.gameID = gameID;
		this.multicastGamePort = multicastGamePort;
	}
	
	public String getCreator() { return this.creator; }
	public int getID() { return this.gameID; }
	public int getPort() { return this.multicastGamePort; }
	
	public String toString() {
		return new String(creator + "  (GAME ID: " + gameID + ")" );
	}

}
