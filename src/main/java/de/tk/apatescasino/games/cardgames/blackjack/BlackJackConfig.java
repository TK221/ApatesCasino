package de.tk.apatescasino.games.cardgames.blackjack;

import de.tk.apatescasino.games.config.GameConfig;
import de.tk.apatescasino.games.config.LocationCoordinates;


public class BlackJackConfig extends GameConfig {

    public LocationCoordinates MainScreenLocation;
    public int MinBet;
    public int MaxBet;

    public int PreparingTime;
    public int TurnTime;

    public BlackJackConfig(GameConfig gameConfig) {
        super(gameConfig.GameID, gameConfig.MinPlayers, gameConfig.MaxPlayers, gameConfig.JoinBlockPosition.GetLocation());
    }
}
