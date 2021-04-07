package de.tk.apatescasino.games;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemStackBuilder {

    public static ItemStack createItemStack(Material material, int amount, String name) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta itemMeta = item.getItemMeta();
        assert itemMeta != null;


        itemMeta.setDisplayName(name);

        item.setItemMeta(itemMeta);
        return item;
    }

    public static ItemStack createItemStack(Material material, int amount, String name, String[] description) {
        ItemStack item = createItemStack(material, amount, name);
        ItemMeta itemMeta = item.getItemMeta();
        assert itemMeta != null;

        List<String> lore = new ArrayList<>();
        for (String s : description) lore.add(ChatColor.GRAY + s);

        itemMeta.setLore(lore);

        item.setItemMeta(itemMeta);

        return item;
    }
}
