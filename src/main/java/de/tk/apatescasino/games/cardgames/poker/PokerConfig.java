package de.tk.apatescasino.games.cardgames.poker;

import de.tk.apatescasino.games.config.GameConfig;
import de.tk.apatescasino.games.config.LocationCoordinates;

public class PokerConfig extends GameConfig {

    public LocationCoordinates MainScreenLocation;
    public int smallBlind;
    public int bigBlind;
    public int minMoney;
    public int maxMoney;
    public double fee;

    public int preparingTime;
    public int turnTime;

    public PokerConfig(GameConfig gameConfig) {
        super(gameConfig.gameID, gameConfig.minPlayers, gameConfig.maxPlayers, gameConfig.joinBlockPosition.getLocation());
    }
}
