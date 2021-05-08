package de.tk.apatescasino.games.cardgames.blackjack;

import de.tk.apatescasino.games.config.GameConfig;
import org.bukkit.Location;


public class BlackJackConfig extends GameConfig {

    public Location MainScreenLocation;
    public int minBet;
    public int maxBet;

    public int preparingTime;
    public int turnTime;

    public BlackJackConfig(GameConfig gameConfig) {
        super(gameConfig.GameID, gameConfig.MinPlayers, gameConfig.MaxPlayers, gameConfig.JoinBlockPosition);
    }
}
