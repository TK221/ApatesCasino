package de.tk.apatescasino.games.config;

import com.google.gson.reflect.TypeToken;
import de.tk.apatescasino.ApatesCasino;
import de.tk.apatescasino.ConfigManager;
import de.tk.apatescasino.games.GameType;
import de.tk.apatescasino.games.cardgames.blackjack.BlackJackConfig;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class GameConfigProvider {


    public void AddNewConfig(GameConfig config) {

        if (config instanceof BlackJackConfig) {
            ConfigManager<BlackJackConfig> configManager = new ConfigManager<>(getGameConfigPath(GameType.BLACKJACK), ApatesCasino.getInstance());

        }
    }

    public List<GameConfig> loadGameConfigs(GameType gameType) throws FileNotFoundException, UnsupportedEncodingException {
        List<GameConfig> gameConfigList = new ArrayList<>();

        switch (gameType) {

            case POKER:
                break;
            case BLACKJACK:
                ConfigManager<List<BlackJackConfig>> configManager = new ConfigManager<>(getGameConfigPath(GameType.BLACKJACK), ApatesCasino.getInstance());
                configManager.loadConfig(new TypeToken<List<BlackJackConfig>>() {
                }.getType());
                gameConfigList.addAll(configManager.getObject());
                break;
        }

        return gameConfigList;
    }

    public void removeConfig(String gameID, GameType gameType) {

    }

    private static String getGameConfigPath(GameType gameType) {
        String path = gameType.toString().toLowerCase();
        return path + ".json";
    }
}
