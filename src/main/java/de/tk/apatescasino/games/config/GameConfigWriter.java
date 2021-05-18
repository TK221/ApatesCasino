package de.tk.apatescasino.games.config;


import org.bukkit.Location;

public interface GameConfigWriter {

    public void addPositions(Location location);

    public void addMessage(String message);
}
