package commonPackage;

import java.io.Serializable;

public class RankingItem implements Serializable{

	private static final long serialVersionUID = 3873024188698404430L;

	public String user;
	public int points;
	
	public RankingItem(String user, int points) {
		this.user = user;
		this.points = points;
	}
	
	public String toString() {
		return user + " - " + points + " Points";
	}
	
}
