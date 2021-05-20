package de.tk.apatescasino.games.cardgames.poker;

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

public class PokerConfigWriter implements GameConfigWriter {
    private enum inputTypes {
        MAINSCREEN,
        SMALLLBLIND,
        BIGBLIND,
        MINMONEY,
        MAXMONEY,
        FEE,
        PREPARINGTIME,
        TURNTIME
    }

    GameConfigManager gameConfigManager;
    private final PokerConfig config;
    private final UUID playerID;

    private inputTypes currInputType;

    public PokerConfigWriter(UUID playerID, GameConfig gameConfig, GameConfigManager gameConfigManager) {
        this.playerID = playerID;
        this.gameConfigManager = gameConfigManager;

        config = new PokerConfig(gameConfig);
        currInputType = inputTypes.values()[0];

        sendInformationMessage();
    }

    @Override
    public void addPositions(Location location) {
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
    public void addMessage(String message) {
        switch (currInputType) {

            case SMALLLBLIND:
                Integer smallBlind = GameConfigManager.convertStringToInteger(message);
                if (smallBlind != null && smallBlind > 0) {
                    config.smallBlind = smallBlind;
                    break;
                }
            case BIGBLIND:
                Integer bigBlind = GameConfigManager.convertStringToInteger(message);
                if (bigBlind != null && bigBlind >= config.smallBlind) {
                    config.bigBlind = bigBlind;
                    break;
                }
            case MINMONEY:
                Integer minMoney = GameConfigManager.convertStringToInteger(message);
                if (minMoney != null && minMoney > 0) {
                    config.minMoney = minMoney;
                    break;
                }
            case MAXMONEY:
                Integer maxMoney = GameConfigManager.convertStringToInteger(message);
                if (maxMoney != null && maxMoney >= config.minMoney) {
                    config.maxMoney = maxMoney;
                    break;
                }
            case FEE:
                Double fee = GameConfigManager.convertStringToDouble(message);
                if (fee != null && fee >= 0 && fee < 1) {
                    config.fee = fee;
                    break;
                }
            case PREPARINGTIME:
                Integer preparingTime = GameConfigManager.convertStringToInteger(message);
                if (preparingTime != null && preparingTime > 0) {
                    config.preparingTime = preparingTime;
                    break;
                }
            case TURNTIME:
                Integer turnTime = GameConfigManager.convertStringToInteger(message);
                if (turnTime != null && turnTime > 0) {
                    config.turnTime = turnTime;
                    break;
                }
                break;
        }

        increaseInputType();
    }

    private void increaseInputType() {
        if (currInputType != null) {
            if (currInputType.ordinal() < inputTypes.values().length - 1) {
                currInputType = inputTypes.values()[currInputType.ordinal() + 1];
                sendInformationMessage();

            } else {
                gameConfigManager.createNewGame(config, playerID);
            }
        }
    }

    private void sendInformationMessage() {
        Player player = Bukkit.getServer().getPlayer(playerID);
        if (player == null) return;

        StringBuilder message = new StringBuilder(ChatColor.AQUA.toString());

        switch (currInputType) {

            case MAINSCREEN:
                message.append("Bitte geben sie die Position der Spielanzeige ein");
                break;
            case SMALLLBLIND:
                message.append("Bitte geben sie den Small-Blind ein");
                break;
            case BIGBLIND:
                message.append("Bitte geben sie den Big-Blind ein");
                break;
            case MINMONEY:
                message.append("Bitte geben sie den Mindesteinsatz für das Spiel ein");
                break;
            case MAXMONEY:
                message.append("Bitte geben sie den Maximaleinsatz für das Spiel ein");
                break;
            case FEE:
                message.append("Bitte geben sie den Gebührenwert in Dezimal ein");
                break;
            case PREPARINGTIME:
                message.append("Bitte geben sie die Vorbereitungszeit ein");
                break;
            case TURNTIME:
                message.append("Bitte geben sie die Spielerzeit ein");
                break;
        }

        player.sendMessage(message.toString());
    }
}
