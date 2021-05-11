package de.tk.apatescasino.games.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;


public class LocationCoordinates {
    public String WorldID;
    public double XCor;
    public double YCor;
    public double ZCor;

    public LocationCoordinates(Location location) {
        this.WorldID = location.getWorld().getUID().toString();
        this.XCor = location.getX();
        this.YCor = location.getY();
        this.ZCor = location.getZ();
    }

    public Location GetLocation() {
        return new Location(Bukkit.getWorld(UUID.fromString(WorldID)), XCor, YCor, ZCor);
    }
}
