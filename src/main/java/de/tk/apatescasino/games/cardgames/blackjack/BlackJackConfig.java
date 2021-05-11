package de.tk.apatescasino.games.cardgames.blackjack;

import de.tk.apatescasino.games.config.GameConfig;
import de.tk.apatescasino.games.config.LocationCoordinates;


public class BlackJackConfig extends GameConfig {

    public LocationCoordinates MainScreenLocation;
    public int minBet;
    public int maxBet;

    public int preparingTime;
    public int turnTime;

    public BlackJackConfig(GameConfig gameConfig) {
        super(gameConfig.GameID, gameConfig.MinPlayers, gameConfig.MaxPlayers, gameConfig.JoinBlockPosition.GetLocation());
    }
}
