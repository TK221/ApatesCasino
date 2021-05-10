package de.tk.apatescasino.games.commands;

import de.tk.apatescasino.games.cardgames.blackjack.BlackJackConfigWriter;
import de.tk.apatescasino.games.cardgames.poker.Poker;
import de.tk.apatescasino.games.config.GameConfig;
import de.tk.apatescasino.games.config.GameConfigManager;
import de.tk.apatescasino.games.lobby.LobbyManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CasinoCommand implements CommandExecutor {

    private final LobbyManager lobbyManager;
    private final GameConfigManager gameConfigManager;

    public CasinoCommand(LobbyManager lobbyManager, GameConfigManager gameConfigManager) {
        this.lobbyManager = lobbyManager;
        this.gameConfigManager = gameConfigManager;
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
                    gameConfigManager.RemoveGame(name, lobbyManager.GetGame(name).getGameType());
                    player.sendMessage(ChatColor.GREEN + name + " Erfolgreich gelöscht");
                } else {
                    player.sendMessage(ChatColor.RED + "Dieses Spiel existiert leider nicht");
                    return true;
                }
            }
        } else if (args.length == 5) {

            if (args[0].equalsIgnoreCase("create")) {
                String name = args[2];

                if (gameConfigManager.PlayerHasConfigWriter(player.getUniqueId())) {
                    player.sendMessage("Sie erstellen bereits ein Spiel");
                    return false;
                } else if (lobbyManager.GameExist(name)) {
                    player.sendMessage(ChatColor.RED + "Das Spiel mit diesem Namen existiert schon");
                    return false;
                } else if (!args[3].matches("[0-9]+") || !args[4].matches("[0-9]+")) {
                    player.sendMessage(ChatColor.RED + "Inkorrekte Eingabe der Mindest- und Maximal Spieleranzahl");
                    return false;
                }

                switch (args[1].toLowerCase()) {
                    case "poker":
                        lobbyManager.AddGame(new Poker(name, facingBlock.getLocation(), 10, 50, 100, 1, 4, 20, 5), name);
                        break;
                    case "blackjack":
                        gameConfigManager.AddConfigWriter(player.getUniqueId(), new BlackJackConfigWriter(player.getUniqueId(),
                                new GameConfig(name, Integer.parseInt(args[3]), Integer.parseInt(args[4]), facingBlock.getLocation()), gameConfigManager));
                        break;

                    default:
                        return false;
                }

                if (lobbyManager.GetGame(name) != null)
                    player.sendMessage(ChatColor.GREEN + name + " Erfolgreich erstellt!");
            }
        }

        return true;
    }
}
