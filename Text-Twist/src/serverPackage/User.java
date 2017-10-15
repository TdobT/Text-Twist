package serverPackage;

import commonPackage.RMI_client_interface;

public class User {
	
	public enum userState {
		ONLINE, OFFLINE, UNACTIVE, DEACTIVATED
	}
	
	private boolean playing;
	private userState state;
	private String userName;
	private byte[] userCodedPassword;
	private byte[] passwordSalt;
	private RMI_client_interface clientCallback;
	
	// For login controls
	private int sessionID = 0;
	
	private int totalPoints;
	
	User(String userName, String userPassword) {
		
		this.userName = userName;
		this.passwordSalt = Passwords.getNextSalt();
		
		char[] charPSW = new char[userPassword.length()];
		userPassword.getChars(0, userPassword.length(), charPSW, 0);
		this.userCodedPassword = 
				Passwords.hash(charPSW, this.passwordSalt);
		
		totalPoints = 0;
		state = userState.OFFLINE;
		playing = false;
	}
	
	// second costructor, used to re-create an user using server state file
	User(String userName, byte[] userCodedPassword, byte[] salt, int points) {
		
		this.userName = userName;
		this.userCodedPassword = userCodedPassword;
		this.passwordSalt = salt;
		this.totalPoints = points;
		state = userState.OFFLINE;
		playing = false;
	}
	
	public int getPoints() { return totalPoints; }
	public synchronized void addPoints(int points) { this.totalPoints += points; }
	public synchronized void resetPoints() { this.totalPoints = 0; }
	
	public String getName() { return this.userName; }
	
	public boolean verifyPassword(String password) { 
		char[] charPSW = new char[password.length()];
		password.getChars(0, password.length(), charPSW, 0);
		return Passwords.isExpectedPassword(charPSW, passwordSalt, userCodedPassword);
		}
	
	public boolean isPlaying() { return this.playing; }
	public userState getState() { return this.state; }
	public synchronized void setState(userState state) { this.state = state; }
	
	public synchronized void resetCallback() { this.clientCallback = null; }
	public RMI_client_interface getCallback() { return this.clientCallback; }
	public synchronized void setCallback(RMI_client_interface clientCallback) {
		this.clientCallback = clientCallback; }

	public synchronized void setSessionID(int id) { this.sessionID = id; }
	public synchronized void resetSessionID () { this.sessionID = 0; }
	public synchronized int getSessionID() { return this.sessionID; }
	
	// necessary to save the server state
	public byte[] getCodedPassword() { return this.userCodedPassword; }
	public byte[] getSalt() { return this.passwordSalt; }
	
	public String toString() {return userName; }
	
}
