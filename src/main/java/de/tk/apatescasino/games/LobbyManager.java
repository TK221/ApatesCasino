package de.tk.apatescasino.games;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.*;

public class LobbyManager {

    public final Map<String, Game> ActiveGames = new HashMap<>();

    public Game getGameByPlayer(UUID playerID) {
        for (Game game : ActiveGames.values()) {
            if (game.containsPlayer(playerID)) return game;
        }
        return null;
    }

    public Game getGameByJoinBlock(Location joinBlockPosition) {
        for (Game game : ActiveGames.values()) {

            if (game.getJoinBlockPosition().equals(joinBlockPosition)) return game;
        }
        return null;
    }
}
