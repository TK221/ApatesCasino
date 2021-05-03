package de.tk.apatescasino.games.cardgames.blackjack;

import de.tk.apatescasino.games.Game;
import de.tk.apatescasino.games.Lobby;
import de.tk.apatescasino.games.cardgames.card.CardDeck;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlackJack implements Game {

    private final int minPlayers;
    private final int maxPlayers;

    private final Map<UUID, BlackJackPlayer> players;
    private final Lobby lobby;

    private final CardDeck cardDeck;

    public BlackJack(String id, int minPlayers, int maxPlayers) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.lobby = new Lobby(maxPlayers, minPlayers, id);

        players = new HashMap<>();
        cardDeck = new CardDeck();
    }


    private void startPreparingTimer() {

    }

    private void startTurnTimer() {

    }

    private void startNewGame() {
        cardDeck.InitStandardDeck();
    }

    public void PlayerAction() {

    }

    private void playerHit() {

    }

    private void playerRest() {

    }

    private void playerBust() {

    }

    @Override
    public void StartGame() {

    }

    @Override
    public void CancelGame() {

    }

    @Override
    public Location getJoinBlockPosition() {
        return null;
    }

    @Override
    public Integer getMaxPlayers() {
        return null;
    }

    @Override
    public Integer getMinPlayers() {
        return null;
    }

    @Override
    public void AddPlayer(Player player) {

    }

    @Override
    public void RemovePlayer(UUID playerID) {

    }

    @Override
    public boolean containsPlayer(UUID playerID) {
        return false;
    }
}
