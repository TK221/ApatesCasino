package de.tk.apatescasino.games;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Lobby {

    public final String ID;

    private final int MaxPlayers;
    private final int MinPlayers;

    private final List<LobbyPlayer> players = new ArrayList<>();


    public Lobby(int maxPlayers, int minPlayers, String id) {
        ID = id;
        MaxPlayers = maxPlayers;
        MinPlayers = minPlayers;
    }

    public void AddPlayer(Player player) {

        if (player != null)
        {
            players.add(new LobbyPlayer(player.getUniqueId(), PlayerState.NEW, player));
        }
    }

    public void RemovePlayer(UUID playerID) {
        players.remove(getPlayerFromList(playerID));
    }

    public LobbyPlayer getPlayer(UUID playerID) {
        return getPlayerFromList(playerID);
    }

    public List<LobbyPlayer> getPlayersByState(PlayerState state) {
        List<LobbyPlayer> playerList = new ArrayList<>();

        for (LobbyPlayer player : players) {
            if (player.getPlayerState() == state) playerList.add(player);
        }

        return playerList;
    }

    public List<LobbyPlayer> getAllPlayers() {
        return players;
    }

    public void ChangePlayerState(UUID playerID, PlayerState state) {
        getPlayerFromList(playerID).setPlayerState(state);
    }

    public int getMaxPlayers() {
        return MaxPlayers;
    }
    public int getMinPlayers() {
        return MinPlayers;
    }

    private LobbyPlayer getPlayerFromList(UUID PlayerID) {
        return players.stream().filter(c -> c.getPlayerID().equals(PlayerID)).findFirst().orElse(null);
    }
}
