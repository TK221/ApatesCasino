package de.tk.apatescasino.games.commands;

import de.tk.apatescasino.games.cardgames.blackjack.BlackJack;
import de.tk.apatescasino.games.cardgames.poker.Poker;
import de.tk.apatescasino.games.lobby.LobbyManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

                if (lobbyManager.GameExist(name)) {
                    lobbyManager.RemoveGame(name);
                    player.sendMessage(ChatColor.GREEN + name + " successfully removed");
                } else {
                    player.sendMessage(ChatColor.RED + "This game doesn't exist");
                    return true;
                }
            }
        } else if (args.length == 3) {

            if (args[0].equalsIgnoreCase("create")) {
                String name = args[2];

                if (lobbyManager.GameExist(name)) {
                    player.sendMessage(ChatColor.RED + "The game with this name already exists");
                    return false;
                }

                switch (args[1].toLowerCase()) {
                    case "poker":
                        lobbyManager.AddGame(new Poker(name, facingBlock.getLocation(), 10, 50, 100, 1, 4, 20, 5), name);
                        break;
                    case "blackjack":
                        lobbyManager.AddGame(new BlackJack(name, 1, 10, facingBlock.getLocation()), name);
                        break;

                    default:
                        return false;
                }

                if (lobbyManager.GetGame(name) != null)
                    player.sendMessage(ChatColor.GREEN + name + " successfully created!");
            }
        }

        return true;
    }
}
