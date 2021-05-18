package de.tk.apatescasino.games.config;

import de.tk.apatescasino.games.Game;
import de.tk.apatescasino.games.GameType;
import de.tk.apatescasino.games.lobby.LobbyManager;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        gameConfigProvider.createGames();
    }

    public void createNewGame(GameConfig gameConfig, Game game, UUID playerID) {
        lobbyManager.addGame(game, gameConfig.gameID);
        gameConfigProvider.addNewConfig(gameConfig);

        configWriterMap.remove(playerID);
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
