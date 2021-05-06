package de.tk.apatescasino.games.utilities;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerInventorySaver {
    static private final Map<UUID, ItemStack[]> playerInventoryMap = new HashMap<>();

    public static void addPlayerInventory(Player player) {
        PlayerInventory playerInventory = player.getInventory();

        ItemStack[] inventory = playerInventory.getContents();
        playerInventory.clear();

        playerInventoryMap.put(player.getUniqueId(), inventory);
    }

    public static void setPlayerInventory(Player player) {
        PlayerInventory playerInventory = player.getInventory();

        ItemStack[] inventory = playerInventoryMap.remove(player.getUniqueId());
        playerInventory.setContents(inventory);

        playerInventoryMap.put(player.getUniqueId(), inventory);
    }
}
