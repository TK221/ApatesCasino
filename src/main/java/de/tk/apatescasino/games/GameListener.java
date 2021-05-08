package de.tk.apatescasino.games;

import de.tk.apatescasino.games.lobby.LobbyManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

public class GameListener implements Listener {
    LobbyManager lobbyManager;

    public GameListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @EventHandler
    public void onPlayerDie(PlayerDeathEvent event) {
        UUID playerID = event.getEntity().getUniqueId();

        Game game = lobbyManager.GetGameByPlayer(playerID);

        if (game != null) {
            game.RemovePlayer(playerID);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();
        Game game = lobbyManager.GetGameByPlayer(playerID);

        if (game != null) {
            game.RemovePlayer(playerID);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();
        Block block = event.getClickedBlock();

        if (event.getHand() == EquipmentSlot.HAND && block != null && lobbyManager.GetGameByJoinBlock(block.getLocation()) != null) {
            if (lobbyManager.GetGameByPlayer(playerID) == null) {
                Game joinBlockGame = lobbyManager.GetGameByJoinBlock(block.getLocation());
                joinBlockGame.AddPlayer(player);
            } else player.sendMessage(ChatColor.YELLOW + "Du bist bereits in einem Spiel");
        }
    }
}
