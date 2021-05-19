package de.tk.apatescasino.games.config;

import com.google.gson.reflect.TypeToken;
import de.tk.apatescasino.ApatesCasino;
import de.tk.apatescasino.ConfigManager;
import de.tk.apatescasino.games.GameType;
import de.tk.apatescasino.games.cardgames.blackjack.BlackJack;
import de.tk.apatescasino.games.cardgames.blackjack.BlackJackConfig;
import de.tk.apatescasino.games.cardgames.poker.Poker;
import de.tk.apatescasino.games.cardgames.poker.PokerConfig;
import de.tk.apatescasino.games.lobby.LobbyManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class GameConfigProvider {

    LobbyManager lobbyManager;


    public GameConfigProvider(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }


    public boolean addNewConfig(GameConfig config) {
        try {
            if (config instanceof PokerConfig) {
                PokerConfig pokerConfig = (PokerConfig) getConfig(config.gameID, GameType.POKER);
                if (pokerConfig == null) {
                    saveConfig(config);
                    return true;
                }

            } else if (config instanceof BlackJackConfig) {
                BlackJackConfig blackJackConfig = (BlackJackConfig) getConfig(config.gameID, GameType.BLACKJACK);
                if (blackJackConfig == null) {
                    saveConfig(config);
                    return true;
                }

            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error while trying to create new game config: " + e.getMessage());
        }

        return false;
    }

    // TODO better Perdomance
    public boolean changeConfigState(String gameID, boolean state) {
        for (GameType gameType : GameType.values()) {

            GameConfig gameConfig = getConfig(gameID, gameType);
            if (gameConfig != null) {
                gameConfig.disabled = state;
                saveConfig(gameConfig);
                return true;
            }
        }

        return false;
    }

    public GameConfig getConfig(String gameID, GameType gameType) {
        try {
            switch (gameType) {

                case POKER:
                    HashMap<String, PokerConfig> pokerConfigs = loadPokerConfigs();
                    return pokerConfigs.get(gameID);
                case BLACKJACK:
                    HashMap<String, BlackJackConfig> blackJackConfigs = loadBlackJackConfigs();
                    return blackJackConfigs.get(gameID);

                default:
                    return null;
            }
        } catch (Exception e) {
            System.out.println("Failed to load game-config: [GameID=" + gameID + "], [GameType=" + gameType + "] - " + e.getMessage());
        }

        return null;
    }

    public boolean saveConfig(GameConfig gameConfig) {
        Objects.requireNonNull(gameConfig);

        String gameID = gameConfig.gameID;

        try {
            if (gameConfig instanceof BlackJackConfig) {
                HashMap<String, BlackJackConfig> blackJackConfigs = loadBlackJackConfigs();
                blackJackConfigs.put(gameID, (BlackJackConfig) gameConfig);

            } else if (gameConfig instanceof PokerConfig) {
                HashMap<String, PokerConfig> pokerConfigs = loadPokerConfigs();
                pokerConfigs.put(gameID, (PokerConfig) gameConfig);
            }
        } catch (
                Exception e) {
            System.out.println("Failed to save game-config: [GameID=" + gameID + "] - " + e.getMessage());
        }

        return false;
    }

    public boolean removeConfig(String gameID, GameType gameType) {
        try {
            switch (gameType) {
                case POKER:
                    HashMap<String, PokerConfig> pokerConfigs = loadPokerConfigs();
                    pokerConfigs.remove(gameID);
                    savePokerConfigs(pokerConfigs);
                    break;
                case BLACKJACK:
                    HashMap<String, BlackJackConfig> blackJackConfigs = loadBlackJackConfigs();
                    blackJackConfigs.remove(gameID);
                    saveBlackJackConfigs(blackJackConfigs);
                    break;
            }
            return true;

        } catch (Exception e) {
            System.out.println("Error while removing game config: " + e.getMessage());
            return false;
        }
    }

    public void createGames() {
        try {
            for (BlackJackConfig config : loadBlackJackConfigs().values().stream().filter(c -> !c.disabled).collect(Collectors.toList()))
                createBlackJackGame(config);
            for (PokerConfig config : loadPokerConfigs().values().stream().filter(c -> !c.disabled).collect(Collectors.toList()))
                createPokerGame(config);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // ---- BlackJack -----
    private HashMap<String, BlackJackConfig> loadBlackJackConfigs() throws IOException {
        ConfigManager<HashMap<String, BlackJackConfig>> configManager = new ConfigManager<>(getGameConfigPath(GameType.BLACKJACK), ApatesCasino.getInstance());
        configManager.loadConfig(new TypeToken<HashMap<String, BlackJackConfig>>() {
        }.getType());

        HashMap<String, BlackJackConfig> blackJackConfigs = configManager.getObject();

        return blackJackConfigs != null ? blackJackConfigs : new HashMap<>();
    }

    private void saveBlackJackConfigs(HashMap<String, BlackJackConfig> blackJackConfigs) {
        ConfigManager<HashMap<String, BlackJackConfig>> configManager = new ConfigManager<>(getGameConfigPath(GameType.BLACKJACK), ApatesCasino.getInstance());
        configManager.setObject(blackJackConfigs);
        configManager.saveConfig();
    }

    private void createBlackJackGame(BlackJackConfig config) {
        lobbyManager.addGame(new BlackJack(config.gameID, config.minPlayers, config.maxPlayers, config.joinBlockPosition.getLocation(),
                config.minBet, config.maxBet, config.preparingTime, config.turnTime), config.gameID);
    }

    // ---- Poker ----
    private HashMap<String, PokerConfig> loadPokerConfigs() throws IOException {
        ConfigManager<HashMap<String, PokerConfig>> configManager = new ConfigManager<>(getGameConfigPath(GameType.POKER), ApatesCasino.getInstance());
        configManager.loadConfig(new TypeToken<HashMap<String, PokerConfig>>() {
        }.getType());

        HashMap<String, PokerConfig> pokerConfigs = configManager.getObject();

        return pokerConfigs != null ? pokerConfigs : new HashMap<>();
    }

    private void savePokerConfigs(HashMap<String, PokerConfig> pokerConfigs) {
        ConfigManager<HashMap<String, PokerConfig>> configManager = new ConfigManager<>(getGameConfigPath(GameType.POKER), ApatesCasino.getInstance());
        configManager.setObject(pokerConfigs);
        configManager.saveConfig();
    }

    private void createPokerGame(PokerConfig config) {
        lobbyManager.addGame(new Poker(config.gameID, config.joinBlockPosition.getLocation(), config.smallBlind, config.bigBlind,
                config.minMoney, config.minPlayers, config.maxPlayers, config.turnTime, config.preparingTime), config.gameID);
    }


    private static String getGameConfigPath(GameType gameType) {
        String path = gameType.toString().toLowerCase();
        return path + ".json";
    }
}
