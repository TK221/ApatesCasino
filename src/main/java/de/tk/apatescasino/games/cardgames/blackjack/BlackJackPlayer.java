package de.tk.apatescasino.games.cardgames.blackjack;

import de.tk.apatescasino.games.cardgames.card.Card;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

class BlackJackPlayer {
    public final Player Player;
    private final List<Card> cards;
    private Integer cardsValue;
    private Integer stake;

    public BlackJackPlayer(Player player) {
        this.Player = player;

        this.cards = new ArrayList<>();
        this.cardsValue = 0;
        this.stake = 0;
    }


    public void AddCard(Card card) {
        cards.add(card);
        calculateCardsValue();
    }

    public List<Card> GetCards() {
        return new ArrayList<>(cards);
    }

    public void ResetCards() {
        cards.clear();
    }

    private void calculateCardsValue() {
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
}
