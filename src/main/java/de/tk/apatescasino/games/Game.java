package de.tk.apatescasino.games;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface Game {

    void startGame();

    void cancelGame();

    GameType getGameType();

    Location getJoinBlockPosition();

    Integer getMaxPlayers();

    Integer getMinPlayers();

    void addPlayer(Player player);

    void removePlayer(UUID playerID);

    boolean containsPlayer(UUID playerID);
}
