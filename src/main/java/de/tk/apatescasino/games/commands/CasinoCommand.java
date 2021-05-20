package de.tk.apatescasino.games.commands;

import de.tk.apatescasino.bank.BankAccountHandler;
import de.tk.apatescasino.games.cardgames.blackjack.BlackJackConfigWriter;
import de.tk.apatescasino.games.cardgames.poker.PokerConfigWriter;
import de.tk.apatescasino.games.config.GameConfig;
import de.tk.apatescasino.games.config.GameConfigManager;
import de.tk.apatescasino.games.lobby.LobbyManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CasinoCommand implements CommandExecutor {

    private final LobbyManager lobbyManager;
    private final GameConfigManager gameConfigManager;
    private final BankAccountHandler bankAccountHandler;

    public CasinoCommand(LobbyManager lobbyManager, GameConfigManager gameConfigManager, BankAccountHandler bankAccountHandler) {
        this.lobbyManager = lobbyManager;
        this.gameConfigManager = gameConfigManager;
        this.bankAccountHandler = bankAccountHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (player == null) return false;
        UUID playerID = player.getUniqueId();
        Block facingBlock = player.getTargetBlock(null, 10);

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("balance")) {
                bankAccountHandler.writeBalance(player);

            } else if (args[0].equalsIgnoreCase("transactions")) {
                bankAccountHandler.writeLastTransactions(player);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("disable")) {
                gameConfigManager.changeGameState(args[1], false);
            } else if (args[0].equalsIgnoreCase("enable")) {
                gameConfigManager.changeGameState(args[1], true);
            } else if (args[0].equalsIgnoreCase("create") && args[1].equalsIgnoreCase("cancel")) {

                if (!gameConfigManager.playerHasConfigWriter(playerID)) {
                    player.sendMessage(ChatColor.RED + "Sie besitzen zurzeit keinen Config-Ersteller");
                }
                gameConfigManager.removeConfigWriter(playerID);

                if (!gameConfigManager.playerHasConfigWriter(playerID))
                    player.sendMessage(ChatColor.GREEN + "Config-Ersteller erfolgreich gelöscht");
                else player.sendMessage(ChatColor.RED + "Fehler beim löschen ihres Config-Erstellers");

            } else if (args[0].equalsIgnoreCase("remove")) {
                String name = args[1];

                if (lobbyManager.gameExist(name)) {
                    gameConfigManager.removeGame(name, lobbyManager.getGame(name).getGameType());
                    player.sendMessage(ChatColor.GREEN + name + " Erfolgreich gelöscht");
                } else {
                    player.sendMessage(ChatColor.RED + "Dieses Spiel existiert leider nicht");
                    return true;
                }
            }
        } else if (args.length == 5) {

            if (args[0].equalsIgnoreCase("create")) {
                String name = args[2];

                if (gameConfigManager.playerHasConfigWriter(playerID)) {
                    player.sendMessage("Sie erstellen bereits ein Spiel");
                    return false;
                } else if (lobbyManager.gameExist(name)) {
                    player.sendMessage(ChatColor.RED + "Das Spiel mit diesem Namen existiert schon");
                    return false;
                } else if (!args[3].matches("[0-9]+") || !args[4].matches("[0-9]+")) {
                    player.sendMessage(ChatColor.RED + "Inkorrekte Eingabe der Mindest- und Maximal Spieleranzahl");
                    return false;
                }

                switch (args[1].toLowerCase()) {
                    case "poker":
                        gameConfigManager.addConfigWriter(playerID, new PokerConfigWriter(playerID,
                                new GameConfig(name, Integer.parseInt(args[3]), Integer.parseInt(args[4]), facingBlock.getLocation()), gameConfigManager));
                        break;
                    case "blackjack":
                        gameConfigManager.addConfigWriter(playerID, new BlackJackConfigWriter(playerID,
                                new GameConfig(name, Integer.parseInt(args[3]), Integer.parseInt(args[4]), facingBlock.getLocation()), gameConfigManager));
                        break;

                    default:
                        return false;
                }

                if (lobbyManager.getGame(name) != null)
                    player.sendMessage(ChatColor.GREEN + name + " Erfolgreich erstellt!");
            }
        }

        return true;
    }
}
