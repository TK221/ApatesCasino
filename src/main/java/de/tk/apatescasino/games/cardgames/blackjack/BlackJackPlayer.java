package de.tk.apatescasino.games.cardgames.blackjack;

import de.tk.apatescasino.games.cardgames.card.Card;
import de.tk.apatescasino.games.cardgames.card.CardRank;
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
    private int cardsValue;
    private int stake;

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

    public void AddMoneyToStake(Integer amount) {
        stake += amount;
    }

    public Integer GetStake() {
        return stake;
    }

    public void ResetStake() {
        stake = 0;
    }

    public Integer GetCardsValue() {
        return cardsValue;
    }

    public static int getCalculatedCardsValue(List<Card> cards) {
        int value = 0;
        for (Card card : cards) value += getValue(card.Rank);

        if (value > 21 && cards.stream().anyMatch(c -> c.Rank.equals(CardRank.ACE))) {
            for (Card card : cards) {
                if (value > 21 && card.Rank.equals(CardRank.ACE)) value -= 10;
            }
        }

        return value;
    }

    private static int getValue(CardRank rank) {
        if (rank == CardRank.ACE) return 11;
        else if (rank.ordinal() < CardRank.TEN.ordinal()) {
            return rank.ordinal() + 2;
        } else return 10;
    }
}
