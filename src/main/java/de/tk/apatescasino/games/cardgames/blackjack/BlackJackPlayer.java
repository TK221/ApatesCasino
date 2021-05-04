package de.tk.apatescasino.games.cardgames.blackjack;

import de.tk.apatescasino.games.cardgames.card.Card;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

enum BlackJackPlayerState {
    PREPARING,
    IN_GAME,
    BETTING
}

class BlackJackPlayer {
    public final Player Player;
    public final Integer PlayerNumber;
    public BlackJackPlayerState state;
    private final List<Card> cards;
    private Integer cardsValue;
    private Integer stake;

    public BlackJackPlayer(Player player, Integer playerNumber) {
        this.Player = player;
        this.PlayerNumber = playerNumber;

        this.cards = new ArrayList<>();
        this.cardsValue = 0;
        this.stake = 0;
        this.state = BlackJackPlayerState.PREPARING;
    }


    public void AddCard(Card card) {
        cards.add(card);
        cardsValue = getCalculatedCardsValue(cards);
    }

    public List<Card> GetCards() {
        return new ArrayList<>(cards);
    }

    public void ResetCards() {
        cards.clear();
        cardsValue = 0;
    }

    public void addMoneyToStake(Integer amount) {
        stake += amount;
    }

    public Integer getStake() {
        return stake;
    }

    public void ResetStake() {
        stake = 0;
    }

    public Integer getCardsValue() {
        return cardsValue;
    }

    public static Integer getCalculatedCardsValue(List<Card> cards) {
        return 0;
    }
}
