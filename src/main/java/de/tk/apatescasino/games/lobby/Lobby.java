package de.tk.apatescasino.games.lobby;

import de.tk.apatescasino.games.PlayerState;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Lobby {

    public final String id;

    private final int maxPlayers;
    private final int minPlayers;

    private final List<LobbyPlayer> players = new ArrayList<>();


    public Lobby(int maxPlayers, int minPlayers, String id) {
        this.id = id;
        this.maxPlayers = maxPlayers;
        this.minPlayers = minPlayers;
    }

    public void addPlayer(Player player) {

        if (player != null) {
            players.add(new LobbyPlayer(player.getUniqueId(), PlayerState.NEW, player));
        }
    }

    public void removePlayer(UUID playerID) {
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

    public void changePlayerState(UUID playerID, PlayerState state) {
        if (getPlayerFromList(playerID) != null) getPlayerFromList(playerID).setPlayerState(state);
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    private LobbyPlayer getPlayerFromList(UUID PlayerID) {
        return players.stream().filter(c -> c.getPlayerID().equals(PlayerID)).findFirst().orElse(null);
    }
}
