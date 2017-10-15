package commonPackage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Multicast_rankings implements Serializable{

	private static final long serialVersionUID = -3754637793233493207L;
	
	private ArrayList<RankingItem> ranks;
	
	public Multicast_rankings(ArrayList<RankingItem> ranks) {
		this.ranks = ranks;
	}

	public void orderRanks() {
		// Sorting
		Collections.sort(ranks, new Comparator<RankingItem>() {
		        @Override
		        public int compare(RankingItem user1, RankingItem user2)
		        {
		        	if (user1.points < user2.points) return 1;
		        	else if (user1.points > user2.points) return -1;
		        	else return 0;
		        }
		    });
	}

	public String[] getStringElements() {
		String[] els = new String[ranks.size()];
		
		for (int i = 0; i < ranks.size(); i++) els[i] = ranks.get(i).toString();
		
		return els;
	}
	
}
