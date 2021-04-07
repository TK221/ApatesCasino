package de.tk.apatescasino.games;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface Game {

    void StartGame();
    void CancelGame();

    Location getJoinBlockPosition();
    Integer getMaxPlayers();
    Integer getMinPlayers();

    void AddPlayer(Player player);
    void RemovePlayer(UUID playerID);
    boolean containsPlayer(UUID playerID);
}
