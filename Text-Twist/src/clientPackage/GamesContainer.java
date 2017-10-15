package clientPackage;

import java.util.ArrayList;

public class GamesContainer {
	
	ArrayList<GamesData> games;
	
	GamesContainer() {
		this.games = new ArrayList<GamesData>();
	}

	public GamesData getGameByID(int gameID) {
		
		for (GamesData game: games) {
			if (game.getID() == gameID) return game;
		}
		return null;
	}
	
	public void addGame(GamesData game) {
		if (game == null) return;
		games.add(game);
	}
	
	public void removeGame(GamesData game) {
		if (game == null) return;
		games.remove(game);
	}
	
}
