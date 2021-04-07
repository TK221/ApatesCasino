package de.tk.apatescasino.games.commands;

import de.tk.apatescasino.games.LobbyManager;
import de.tk.apatescasino.games.cardgames.Poker;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public class CasinoCommand implements CommandExecutor {

    private final LobbyManager lobbyManager;

    public CasinoCommand(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (player == null) return false;
        Block facingBlock = player.getTargetBlock(null, 10);

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove")) {
                String name = args[1];

                if (lobbyManager.ActiveGames.containsKey(name)) {
                    lobbyManager.ActiveGames.remove(name);
                    player.sendMessage(ChatColor.GREEN + name + " successfully removed");
                } else {
                    player.sendMessage(ChatColor.RED + "This game doesn't exist");
                    return true;
                }
            }
        }

        else if (args.length == 3) {

            if (args[0].equalsIgnoreCase("create")) {
                String name = args[2];

                if (lobbyManager.ActiveGames.containsKey(name)) {
                    player.sendMessage(ChatColor.RED + "The game with this name already exists");
                    return false;
                }

                switch (args[1].toLowerCase()) {
                case "poker":
                    lobbyManager.ActiveGames.put(name, new Poker(facingBlock.getLocation(), 5, 10, 100, 2, 4));
                    break;

                    default:
                        return false;
                }

                if (lobbyManager.ActiveGames.get(name) != null) player.sendMessage(ChatColor.GREEN + name + " successfully created!");
            }
        }

        return true;
    }
}
