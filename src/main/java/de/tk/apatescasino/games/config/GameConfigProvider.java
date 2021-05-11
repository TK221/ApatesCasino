package de.tk.apatescasino.games.config;

import com.google.gson.reflect.TypeToken;
import de.tk.apatescasino.ApatesCasino;
import de.tk.apatescasino.ConfigManager;
import de.tk.apatescasino.games.GameType;
import de.tk.apatescasino.games.cardgames.blackjack.BlackJack;
import de.tk.apatescasino.games.cardgames.blackjack.BlackJackConfig;
import de.tk.apatescasino.games.lobby.LobbyManager;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class GameConfigProvider {

    LobbyManager lobbyManager;


    public GameConfigProvider(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }


    public boolean AddNewConfig(GameConfig config) {
        if (config instanceof BlackJackConfig) {
            try {
                HashMap<String, BlackJackConfig> blackJackConfigs = loadBlackJackConfigs();

                if (!blackJackConfigs.containsKey(config.GameID))
                    blackJackConfigs.put(config.GameID, (BlackJackConfig) config);

                saveBlackJackConfigs(blackJackConfigs);
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    public boolean RemoveConfig(String gameID, GameType gameType) {
        switch (gameType) {

            case POKER:
                break;
            case BLACKJACK:
                try {
                    HashMap<String, BlackJackConfig> blackJackConfigs = loadBlackJackConfigs();
                    blackJackConfigs.remove(gameID);
                    saveBlackJackConfigs(blackJackConfigs);
                } catch (Exception e) {
                    return false;
                }
                break;
        }

        return true;
    }

    public void CreateGames() {
        try {
            for (BlackJackConfig config : loadBlackJackConfigs().values()) createBlackJackGame(config);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    private HashMap<String, BlackJackConfig> loadBlackJackConfigs() throws FileNotFoundException, UnsupportedEncodingException {
        ConfigManager<HashMap<String, BlackJackConfig>> configManager = new ConfigManager<>(getGameConfigPath(GameType.BLACKJACK), ApatesCasino.getInstance());
        configManager.loadConfig(new TypeToken<HashMap<String, BlackJackConfig>>() {
        }.getType());

        return configManager.getObject() != null ? configManager.getObject() : new HashMap<>();
    }

    private void saveBlackJackConfigs(HashMap<String, BlackJackConfig> blackJackConfigs) {
        ConfigManager<HashMap<String, BlackJackConfig>> configManager = new ConfigManager<>("blackjack.json", ApatesCasino.getInstance());
        configManager.setObject(blackJackConfigs);
        configManager.saveConfig();
    }

    private void createBlackJackGame(BlackJackConfig config) {
        lobbyManager.AddGame(new BlackJack(config.GameID, config.MinPlayers, config.MaxPlayers, config.JoinBlockPosition.GetLocation()), config.GameID);
    }

    private static String getGameConfigPath(GameType gameType) {
        String path = gameType.toString().toLowerCase();
        return path + ".json";
    }
}
