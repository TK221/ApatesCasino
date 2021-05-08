package de.tk.apatescasino.games.config;

import org.bukkit.Location;

public class GameConfig {
    public String GameID;
    public int MinPlayers;
    public int MaxPlayers;
    public Location JoinBlockPosition;

    public GameConfig(String gameID, int minPlayers, int maxPlayers, Location joinBlockPosition) {
        GameID = gameID;
        MinPlayers = minPlayers;
        MaxPlayers = maxPlayers;
        JoinBlockPosition = joinBlockPosition;
    }
}
