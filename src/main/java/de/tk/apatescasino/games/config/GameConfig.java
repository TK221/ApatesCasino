package de.tk.apatescasino.games.config;

import org.bukkit.Location;

public class GameConfig {
    public String gameID;
    public int minPlayers;
    public int maxPlayers;
    public LocationCoordinates joinBlockPosition;

    public GameConfig(String gameID, int minPlayers, int maxPlayers, Location joinBlockPosition) {
        this.gameID = gameID;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.joinBlockPosition = new LocationCoordinates(joinBlockPosition);
    }
}
