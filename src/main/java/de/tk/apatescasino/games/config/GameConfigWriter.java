package de.tk.apatescasino.games.config;


import org.bukkit.Location;

public interface GameConfigWriter {

    public void AddPositions(Location location);

    public void AddMessage(String message);
}
