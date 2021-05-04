package de.tk.apatescasino.games.cardgames.poker;

import de.tk.apatescasino.games.Game;
import de.tk.apatescasino.games.LobbyManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

public class PokerListener implements Listener {

    private final LobbyManager lobbyManager;


    public PokerListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerID = event.getPlayer().getUniqueId();
        Block block = event.getClickedBlock();
        event.getPlayer().getInventory().getHeldItemSlot();

        Game game = lobbyManager.getGameByPlayer(playerID);

        if (event.getHand() == EquipmentSlot.HAND) {
            if (game instanceof Poker) {
                ((Poker) game).PlayerAction(playerID, event.getPlayer().getInventory().getHeldItemSlot());
            } else if (block != null) {
                Game newGame = lobbyManager.getGameByJoinBlock(block.getLocation());

                if (newGame != null) {
                    if (!newGame.containsPlayer(player.getUniqueId())) newGame.AddPlayer(player);
                    else player.sendMessage(ChatColor.YELLOW + "You are already in the game");
                }
            }
        }

    }

    @EventHandler
    public void onPlayerMessage(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();
        String message = event.getMessage();

        Game game = lobbyManager.getGameByPlayer(playerID);

        if (game instanceof Poker) {
            ((Poker) game).OnPlayerSendMessage(playerID, message);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        UUID playerID = event.getWhoClicked().getUniqueId();

        Game game = lobbyManager.getGameByPlayer(playerID);
        if (game instanceof Poker) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPickUpItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            UUID playerID = player.getUniqueId();

            Game game = lobbyManager.getGameByPlayer(playerID);
            if (game instanceof Poker) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();

        Game game = lobbyManager.getGameByPlayer(playerID);

        if (game instanceof Poker) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();

        Game game = lobbyManager.getGameByPlayer(playerID);

        if (game instanceof Poker) {
            event.setCancelled(true);
        }
    }
}
