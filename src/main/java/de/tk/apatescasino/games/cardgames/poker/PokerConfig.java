package de.tk.apatescasino.games.cardgames.poker;

import de.tk.apatescasino.games.config.GameConfig;
import de.tk.apatescasino.games.config.LocationCoordinates;

public class PokerConfig extends GameConfig {

    public LocationCoordinates MainScreenLocation;
    public int SmallBlind;
    public int BigBlind;
    public int MinMoney;
    public int MaxMoney;
    public double Fee;

    public int PreparingTime;
    public int TurnTime;

    public PokerConfig(GameConfig gameConfig) {
        super(gameConfig.GameID, gameConfig.MinPlayers, gameConfig.MaxPlayers, gameConfig.JoinBlockPosition.GetLocation());
    }
}
