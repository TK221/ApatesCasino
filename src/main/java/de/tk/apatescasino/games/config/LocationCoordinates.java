package de.tk.apatescasino.games.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;


public class LocationCoordinates {
    public String worldID;
    public double xCor;
    public double yCor;
    public double zCor;

    public LocationCoordinates(Location location) {
        this.worldID = location.getWorld().getUID().toString();
        this.xCor = location.getX();
        this.yCor = location.getY();
        this.zCor = location.getZ();
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(UUID.fromString(worldID)), xCor, yCor, zCor);
    }
}
