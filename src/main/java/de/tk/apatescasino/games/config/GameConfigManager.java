package de.tk.apatescasino.games.config;

import de.tk.apatescasino.games.Game;
import de.tk.apatescasino.games.lobby.LobbyManager;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameConfigManager {

    LobbyManager lobbyManager;
    Map<UUID, GameConfigWriter> configWriterMap;


    public GameConfigManager(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;

        configWriterMap = new HashMap<>();
    }

    public void CreateNewGame(GameConfig gameConfig, Game game) {
        lobbyManager.AddGame(game, gameConfig.GameID);
    }

    public void PlayerSendMessage(UUID playerID, String message) {
        if (configWriterMap.containsKey(playerID)) configWriterMap.get(playerID).AddMessage(message);
    }

    public void PlayerSendLocation(UUID playerID, Location location) {
        if (configWriterMap.containsKey(playerID)) configWriterMap.get(playerID).AddPositions(location);
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
