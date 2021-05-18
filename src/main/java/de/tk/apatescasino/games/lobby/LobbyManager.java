package de.tk.apatescasino.games.lobby;

import de.tk.apatescasino.games.Game;
import org.bukkit.Location;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LobbyManager {

    private final Map<String, Game> activeGames;

    public LobbyManager() {
        activeGames = new HashMap<>();
    }

    public Game getGameByPlayer(UUID playerID) {
        for (Game game : activeGames.values()) {
            if (game.containsPlayer(playerID)) return game;
        }
        return null;
    }

    public Game getGameByJoinBlock(Location joinBlockPosition) {
        for (Game game : activeGames.values()) {

            if (game.getJoinBlockPosition().equals(joinBlockPosition)) return game;
        }
        return null;
    }

    public boolean gameExist(String gameID) {
        return activeGames.containsKey(gameID);
    }

    public Game getGame(String gameID) {
        return activeGames.get(gameID);
    }

    public Collection<Game> getAllGames() {
        return activeGames.values();
    }

    public void addGame(Game game, String gameID) {
        if (!activeGames.containsKey(gameID)) activeGames.put(gameID, game);
    }

    public void removeGame(String gameID) {
        Game game = activeGames.get(gameID);
        game.cancelGame();

        activeGames.remove(gameID);
    }
}
