package de.tk.apatescasino.games.config;

import de.tk.apatescasino.games.Game;
import de.tk.apatescasino.games.GameType;
import de.tk.apatescasino.games.cardgames.blackjack.BlackJack;
import de.tk.apatescasino.games.cardgames.blackjack.BlackJackConfig;
import de.tk.apatescasino.games.cardgames.poker.Poker;
import de.tk.apatescasino.games.cardgames.poker.PokerConfig;
import de.tk.apatescasino.games.lobby.LobbyManager;
import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;

public class GameConfigManager {

    LobbyManager lobbyManager;
    GameConfigProvider gameConfigProvider;
    Map<UUID, GameConfigWriter> configWriterMap;


    public GameConfigManager(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;

        configWriterMap = new HashMap<>();
        gameConfigProvider = new GameConfigProvider(lobbyManager);
    }

    public void createAllGames() {
        for (GameType gameType : GameType.values()) {
            gameConfigProvider.getConfigList(gameType)
                    .stream().filter(c -> !c.disabled).collect(Collectors.toList())
                    .forEach(this::createGame);
        }
    }

    public void createNewGame(GameConfig gameConfig, UUID playerID) {
        createGame(gameConfig);
        gameConfigProvider.addNewConfig(gameConfig);

        configWriterMap.remove(playerID);
    }

    public void createGame(GameConfig gameConfig) {
        String gameID = gameConfig.gameID;

        if (gameConfig instanceof PokerConfig) {
            lobbyManager.addGame(Poker.createGame((PokerConfig) gameConfig), gameID);
        } else if (gameConfig instanceof BlackJackConfig) {
            lobbyManager.addGame(BlackJack.createGame((BlackJackConfig) gameConfig), gameID);
        }
    }

    public void changeGameState(String gameID, boolean state) {
        if (state) {
            gameConfigProvider.changeConfigState(gameID, true);
            // TODO better
            for (GameType gameType : GameType.values()) {
                GameConfig gameConfig = gameConfigProvider.getConfig(gameID, gameType);
                if (gameConfig != null) {
                    createGame(gameConfig);
                }
            }

        } else {
            lobbyManager.removeGame(gameID);
            gameConfigProvider.changeConfigState(gameID, false);
        }

    }

    public void removeGame(String gameID, GameType gameType) {
        lobbyManager.removeGame(gameID);
        gameConfigProvider.removeConfig(gameID, gameType);
    }

    public void playerSendMessage(UUID playerID, String message) {
        if (configWriterMap.containsKey(playerID)) configWriterMap.get(playerID).addMessage(message);
    }

    public void playerSendLocation(UUID playerID, Location location) {
        if (configWriterMap.containsKey(playerID)) configWriterMap.get(playerID).addPositions(location);
    }

    public boolean playerHasConfigWriter(UUID playerID) {
        return configWriterMap.containsKey(playerID);
    }

    public boolean addConfigWriter(UUID playerID, GameConfigWriter configWriter) {
        if (configWriterMap.containsKey(playerID)) return false;

        configWriterMap.put(playerID, configWriter);
        return true;
    }

    public void removeConfigWriter(UUID playerID) {
        configWriterMap.remove(playerID);
    }

    public static Integer convertStringToInteger(String message) {
        if (message.matches("[0-9]+")) {
            return Integer.parseInt(message);
        } else return null;
    }

    public static Double convertStringToDouble(String message) {
        if (message.matches("(\\-?\\d*\\.?\\d+)")) {
            return Double.parseDouble(message);
        } else return null;
    }
}
