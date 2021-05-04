package de.tk.apatescasino.games;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class GameListener implements Listener {
    LobbyManager lobbyManager;

    public GameListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @EventHandler
    public void onPlayerDie(PlayerDeathEvent event) {
        UUID playerID = event.getEntity().getUniqueId();

        Game game = lobbyManager.getGameByPlayer(playerID);

        if (game != null) {
            game.RemovePlayer(playerID);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();

        Game game = lobbyManager.getGameByPlayer(playerID);

        if (game != null) {
            game.RemovePlayer(playerID);
        }
    }
}
