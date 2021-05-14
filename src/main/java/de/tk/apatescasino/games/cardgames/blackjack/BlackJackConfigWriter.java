package de.tk.apatescasino.games.cardgames.blackjack;

import de.tk.apatescasino.games.Game;
import de.tk.apatescasino.games.config.GameConfig;
import de.tk.apatescasino.games.config.GameConfigManager;
import de.tk.apatescasino.games.config.GameConfigWriter;
import de.tk.apatescasino.games.config.LocationCoordinates;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;


public class BlackJackConfigWriter implements GameConfigWriter {

    private enum inputTypes {
        MAINSCREEN,
        MINBET,
        MAXBET,
        PREPARINGTIME,
        TURNTIME
    }

    GameConfigManager gameConfigManager;
    private final BlackJackConfig config;
    private final UUID playerID;

    private inputTypes currInputType;

    public BlackJackConfigWriter(UUID playerID, GameConfig gameConfig, GameConfigManager gameConfigManager) {
        this.playerID = playerID;
        this.gameConfigManager = gameConfigManager;

        config = new BlackJackConfig(gameConfig);
        currInputType = inputTypes.values()[0];

        sendInformationMessage();
    }

    @Override
    public void AddPositions(Location location) {
        switch (currInputType) {
            case MAINSCREEN:
                config.MainScreenLocation = new LocationCoordinates(location);
                break;
            default:
                sendInformationMessage();
                return;
        }

        increaseInputType();
    }

    @Override
    public void AddMessage(String message) {
        switch (currInputType) {
            case MINBET:
                Integer minBet = GameConfigManager.convertStringToInteger(message);
                if (minBet != null && minBet >= 0) {
                    config.minBet = minBet;
                    break;
                }
            case MAXBET:
                Integer maxBet = GameConfigManager.convertStringToInteger(message);
                if (maxBet != null && maxBet >= config.minBet) {
                    config.maxBet = maxBet;
                    break;
                }
            case PREPARINGTIME:
                Integer preparingTime = GameConfigManager.convertStringToInteger(message);
                if (preparingTime != null && preparingTime >= 0) {
                    config.preparingTime = preparingTime;
                    break;
                }
            case TURNTIME:
                Integer turnTime = GameConfigManager.convertStringToInteger(message);
                if (turnTime != null && turnTime >= 0) {
                    config.turnTime = turnTime;
                    break;
                }

            default:
                sendInformationMessage();
                return;
        }

        increaseInputType();
    }

    private void increaseInputType() {
        if (currInputType != null) {
            if (currInputType.ordinal() < inputTypes.values().length - 1)
                currInputType = inputTypes.values()[currInputType.ordinal() + 1];
            else {
                Game game = new BlackJack(config.GameID, config.MinPlayers, config.MaxPlayers, config.JoinBlockPosition.GetLocation(),
                        config.minBet, config.maxBet, config.preparingTime, config.turnTime);
                gameConfigManager.CreateNewGame(config, game, playerID);
            }
        }
        sendInformationMessage();
    }

    private void sendInformationMessage() {
        Player player = Bukkit.getServer().getPlayer(playerID);
        if (player == null) return;

        StringBuilder message = new StringBuilder(ChatColor.AQUA.toString());

        switch (currInputType) {
            case MAINSCREEN:
                message.append("Bitte geben sie die Position der Spielanzeige ein");
                break;
            case MINBET:
                message.append("Bitte geben sie den Mindesteinsatz ein");
                break;
            case MAXBET:
                message.append("Bitte geben sie den Maximaleinsatz ein");
                break;
            case PREPARINGTIME:
                message.append("Bitte geben sie die Runden-Wartezeit ein");
                break;
            case TURNTIME:
                message.append("Bitte geben sie die Spieler-Zugzeit ein");
                break;
            default:
                break;
        }

        player.sendMessage(message.toString());
    }
}
