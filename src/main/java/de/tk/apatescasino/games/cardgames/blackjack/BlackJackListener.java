package de.tk.apatescasino.games.cardgames.blackjack;

import de.tk.apatescasino.games.Game;
import de.tk.apatescasino.games.lobby.LobbyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

public class BlackJackListener implements Listener {
    LobbyManager lobbyManager;

    public BlackJackListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }


    @EventHandler
    public void onPlayerMessage(AsyncPlayerChatEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();
        String message = event.getMessage();

        Game game = lobbyManager.GetGameByPlayer(playerID);

        if (game instanceof BlackJack) {
            ((BlackJack) game).OnPlayerSendMessage(playerID, message);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();
        int slot = player.getInventory().getHeldItemSlot();

        if (event.getHand() == EquipmentSlot.HAND) {
            Game playerGame = lobbyManager.GetGameByPlayer(playerID);

            if (playerGame instanceof BlackJack) {
                ((BlackJack) playerGame).PlayerAction(playerID, slot);
            }
        }
    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();

        Game game = lobbyManager.GetGameByPlayer(playerID);

        if (game instanceof BlackJack) {
            event.setCancelled(true);
        }
    }
}
