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


    public void addCard(Card card) {
        cards.add(card);
        cardsValue = getCalculatedCardsValue(cards);
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    public void resetCards() {
        cards.clear();
        cardsValue = 0;
    }

    public void addMoneyToStake(Integer amount) {
        stake += amount;
    }

    public Integer getStake() {
        return stake;
    }

    public void resetStake() {
        stake = 0;
    }

    public Integer getCardsValue() {
        return cardsValue;
    }

    public static int getCalculatedCardsValue(List<Card> cards) {
        int value = 0;
        for (Card card : cards) value += getValue(card.rank);

        if (value > 21 && cards.stream().anyMatch(c -> c.rank.equals(CardRank.ACE))) {
            for (Card card : cards) {
                if (value > 21 && card.rank.equals(CardRank.ACE)) value -= 10;
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
