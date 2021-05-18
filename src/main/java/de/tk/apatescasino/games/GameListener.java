package de.tk.apatescasino.games;

import de.tk.apatescasino.ApatesCasino;
import de.tk.apatescasino.games.config.GameConfigManager;
import de.tk.apatescasino.games.lobby.LobbyManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class GameListener implements Listener {
    LobbyManager lobbyManager;
    GameConfigManager gameConfigManager;

    public GameListener(LobbyManager lobbyManager, GameConfigManager gameConfigManager) {
        this.lobbyManager = lobbyManager;
        this.gameConfigManager = gameConfigManager;
    }

    @EventHandler
    public void onPlayerDie(PlayerDeathEvent event) {
        UUID playerID = event.getEntity().getUniqueId();

        Game game = lobbyManager.getGameByPlayer(playerID);

        if (game != null) {
            game.removePlayer(playerID);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();
        Game game = lobbyManager.getGameByPlayer(playerID);

        if (game != null) {
            game.removePlayer(playerID);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();
        Block block = event.getClickedBlock();

        if (event.getHand() == EquipmentSlot.HAND && block != null) {
            if (gameConfigManager.playerHasConfigWriter(playerID)) {
                gameConfigManager.playerSendLocation(playerID, block.getLocation());

            } else if (lobbyManager.getGameByJoinBlock(block.getLocation()) != null) {
                if (lobbyManager.getGameByPlayer(playerID) == null) {
                    Game joinBlockGame = lobbyManager.getGameByJoinBlock(block.getLocation());
                    joinBlockGame.addPlayer(player);
                } else player.sendMessage(ChatColor.YELLOW + "Du bist bereits in einem Spiel");
            }
        }
    }

    @EventHandler
    public void onPlayerMessage(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (ApatesCasino.getChatMessageHandler().handleChat(player, message)) event.setCancelled(true);

        if (gameConfigManager.playerHasConfigWriter(player.getUniqueId())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    gameConfigManager.playerSendMessage(player.getUniqueId(), message);
                }
            }.runTask(ApatesCasino.getInstance());

            event.setCancelled(true);
        }
    }
}
