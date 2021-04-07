package de.tk.apatescasino.games;

import org.bukkit.entity.Player;

import java.util.UUID;

public class LobbyPlayer {

    private final UUID playerID;
    private PlayerState playerState;
    private final Player player;

    public LobbyPlayer(UUID playerID, PlayerState playerState, Player player)
    {
        this.playerID = playerID;
        this.playerState = playerState;
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getPlayerID() {
        return playerID;
    }

    public PlayerState getPlayerState() {
        return playerState;
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = playerState;
    }
}
