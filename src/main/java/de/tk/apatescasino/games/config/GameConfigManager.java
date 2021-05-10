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
        gameConfigProvider = new GameConfigProvider();
    }

    public void CreateNewGame(GameConfig gameConfig, Game game, UUID playerID) {
        lobbyManager.AddGame(game, gameConfig.GameID);
        gameConfigProvider.AddNewConfig(gameConfig);

        configWriterMap.remove(playerID);
    }

    public void RemoveGame(String gameID, GameType gameType) {
        lobbyManager.RemoveGame(gameID);
        gameConfigProvider.RemoveConfig(gameID, gameType);
    }

    public void PlayerSendMessage(UUID playerID, String message) {
        if (configWriterMap.containsKey(playerID)) configWriterMap.get(playerID).AddMessage(message);
    }

    public void PlayerSendLocation(UUID playerID, Location location) {
        if (configWriterMap.containsKey(playerID)) configWriterMap.get(playerID).AddPositions(location);
    }

    public boolean PlayerHasConfigWriter(UUID playerID) {
        return configWriterMap.containsKey(playerID);
    }

    public boolean AddConfigWriter(UUID playerID, GameConfigWriter configWriter) {
        if (configWriterMap.containsKey(playerID)) return false;

        configWriterMap.put(playerID, configWriter);
        return true;
    }

    public void RemoveConfigWriter(UUID playerID) {
        configWriterMap.remove(playerID);
    }

    public static Integer convertStringToInteger(String message) {
        if (message.matches("[0-9]+")) {
            return Integer.parseInt(message);
        } else return null;
    }
}
