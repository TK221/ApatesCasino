package de.tk.apatescasino.games.cardgames.poker;

import de.tk.apatescasino.games.cardgames.card.Card;
import de.tk.apatescasino.games.cardgames.card.CardDeck;
import de.tk.apatescasino.games.cardgames.card.CardRank;
import de.tk.apatescasino.games.cardgames.card.CardType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

enum PokerHand {
    HIGH_CARD,
    PAIR,
    TWO_PAIR,
    THREE_OF_A_KIND,
    STRAIGHT,
    FLUSH,
    FULL_HOUSE,
    FOUR_OF_A_KIND,
    STRAIGHT_FLUSH,
    ROYAL_FLUSH
}

class PlayerPokerHand {
    public Card FirstCard;
    public Card SecondCard;
    public PokerHand Hand;
    public Integer HandScore;
}

public class PokerCardHandler {

    // Current card deck
    public CardDeck deck;
    // Cards which are showed on the table
    public List<Card> showedCards;


    public void InitGameCardHandler() {
        // Initialize card deck and shuffle it
        deck = new CardDeck();
        deck.InitStandardDeck();
        deck.ShuffleDeck();

        // Show three cards to the table
        showedCards = new ArrayList<>();
    }

    public void InitPlayerPokerCards(PlayerPokerHand hand) {
        hand.FirstCard = deck.pickFirst();
        hand.SecondCard = deck.pickFirst();
    }

    public List<PokerPlayerProperties> getWinners(List<PokerPlayerProperties> players) {
        for (PokerPlayerProperties player : players) {
            calculatePlayerHand(player.hand);
            System.out.println("Hand: " + player.hand.Hand.name());
        }

        players.sort(Comparator.comparing(p -> p.hand.Hand.ordinal()));
        Collections.reverse(players);

        List<PokerPlayerProperties> potentialPlayers = players.stream().filter(h -> h.hand.Hand.ordinal() == players.get(0).hand.Hand.ordinal()).sorted(Comparator.comparing(s -> s.hand.HandScore)).collect(Collectors.toList());
        Collections.reverse(potentialPlayers);
        List<PokerPlayerProperties> highestScorePlayers = potentialPlayers.stream().filter(p -> p.hand.HandScore.equals(potentialPlayers.get(0).hand.HandScore)).collect(Collectors.toList());

        System.out.println(highestScorePlayers);

        List<PokerPlayerProperties> highestFirstCardPlayers = highestScorePlayers.stream().filter(p -> p.hand.FirstCard.Rank.ordinal() == highestScorePlayers.get(0).hand.FirstCard.Rank.ordinal()).sorted(Comparator.comparing(c -> c.hand.FirstCard.Rank.ordinal())).collect(Collectors.toList());
        Collections.reverse(highestFirstCardPlayers);
        List<PokerPlayerProperties> highestSecondCardPlayers = highestScorePlayers.stream().filter(p -> p.hand.SecondCard.Rank.ordinal() == highestScorePlayers.get(0).hand.SecondCard.Rank.ordinal()).sorted(Comparator.comparing(c -> c.hand.SecondCard.Rank.ordinal())).collect(Collectors.toList());
        Collections.reverse(highestSecondCardPlayers);

        List<PokerPlayerProperties> winners = new ArrayList<>();
        System.out.println("FirstCard: " + highestFirstCardPlayers);
        System.out.println("SecondCard: " + highestSecondCardPlayers);
        if (highestFirstCardPlayers.get(0).hand.FirstCard.Rank.ordinal() >= highestSecondCardPlayers.get(0).hand.SecondCard.Rank.ordinal()) {
            winners.addAll(highestFirstCardPlayers.stream().filter(p -> p.hand.FirstCard.Rank.ordinal() == highestFirstCardPlayers.get(0).hand.FirstCard.Rank.ordinal()).collect(Collectors.toList()));
        }
        if (highestFirstCardPlayers.get(0).hand.FirstCard.Rank.ordinal() <= highestSecondCardPlayers.get(0).hand.SecondCard.Rank.ordinal()) {
            winners.addAll(highestSecondCardPlayers.stream().filter(p -> p.hand.SecondCard.Rank.ordinal() == highestSecondCardPlayers.get(0).hand.SecondCard.Rank.ordinal()).collect(Collectors.toList()));
        }

        return winners;
    }

    private void calculatePlayerHand(PlayerPokerHand playerHand) {
        if (playerHand == null) return;

        List<Card> allCards = new ArrayList<>(showedCards);
        allCards.add(playerHand.FirstCard);
        allCards.add(playerHand.SecondCard);
        allCards.sort(Comparator.comparing(c -> c.Rank.ordinal()));

        List<List<Card>> sameTypeCards = getSameTypeCards(allCards);
        List<List<Card>> sameRankCards = getSameRankCards(allCards);

        List<Card> finalCards;

        // Royal FLush
        if (sameTypeCards.stream().anyMatch(l -> l.size() >= 5)) {
            List<Card> cards = sameTypeCards.stream().filter(l -> l.size() >= 5).findFirst().orElse(new ArrayList<>());
            cards.sort(Comparator.comparing(c -> c.Rank.ordinal()));

            finalCards = new ArrayList<>();
            int index = CardRank.TEN.ordinal();

            for (Card card : cards) {
                if (card.Rank.ordinal() == index) {
                    index++;
                    finalCards.add(card);
                }
            }
            if (index > CardRank.ACE.ordinal()) {
                playerHand.HandScore = getHandValue(finalCards, PokerHand.ROYAL_FLUSH);
                playerHand.Hand = PokerHand.ROYAL_FLUSH;
                return;
            }
        }

        // Straight Flush
        if (sameTypeCards.stream().anyMatch(l -> l.size() >= 5)) {
            List<Card> cards = sameTypeCards.stream().filter(l -> l.size() >= 5).findFirst().orElse(new ArrayList<>());

            finalCards = getHighestStraight(cards);
            if (finalCards != null) {
                playerHand.HandScore = getHandValue(finalCards, PokerHand.STRAIGHT_FLUSH);
                playerHand.Hand = PokerHand.STRAIGHT_FLUSH;
                return;
            }
        }

        // Four of a kind
        if (sameRankCards.stream().anyMatch(l -> l.size() == 4)) {
            List<Card> cards = sameRankCards.stream().filter(l -> l.size() == 4).findFirst().orElse(new ArrayList<>());

            playerHand.HandScore = getHandValue(cards, PokerHand.FOUR_OF_A_KIND);
            playerHand.Hand = PokerHand.FOUR_OF_A_KIND;
            return;
        }

        // Full House
        if (sameRankCards.stream().anyMatch(l -> l.size() == 3) && sameRankCards.stream().anyMatch(l -> l.size() == 2)) {
            List<List<Card>> threeOfAKind = sameRankCards.stream().filter(l -> l.size() == 3).collect(Collectors.toList());
            List<List<Card>> pairs = sameRankCards.stream().filter(l -> l.size() == 2).collect(Collectors.toList());

            threeOfAKind.sort(Comparator.comparing(l -> l.get(0).Rank.ordinal()));
            pairs.sort(Comparator.comparing(l -> l.get(0).Rank.ordinal()));

            finalCards = threeOfAKind.get(0);
            finalCards.addAll(pairs.get(0));

            playerHand.HandScore = getHandValue(finalCards, PokerHand.FULL_HOUSE);
            playerHand.Hand = PokerHand.FULL_HOUSE;
            return;
        }

        // Flush
        if (sameTypeCards.stream().anyMatch(l -> l.size() >= 5)) {
            List<Card> cards = sameTypeCards.stream().filter(l -> l.size() >= 5).findFirst().orElse(new ArrayList<>());
            cards.sort(Comparator.comparing(c -> c.Rank.ordinal()));

            if (cards.size() > 5) {
                cards.subList(0, (cards.size() - 5)).clear();
            }

            playerHand.HandScore = getHandValue(cards, PokerHand.FLUSH);
            playerHand.Hand = PokerHand.FLUSH;
            return;
        }

        // Straight
        if (allCards.size() >= 5) {
            List<Card> cards = new ArrayList<>(allCards);

            finalCards = getHighestStraight(cards);

            if (finalCards != null) {
                playerHand.HandScore = getHandValue(cards, PokerHand.STRAIGHT);
                playerHand.Hand = PokerHand.STRAIGHT;
                return;
            }
        }

        // Three of a kind
        if (sameRankCards.stream().anyMatch(l -> l.size() == 3)) {
            List<List<Card>> threeOfAKind = sameRankCards.stream().filter(l -> l.size() == 3).sorted(Comparator.comparing(l -> l.get(0).Rank.ordinal())).collect(Collectors.toList());

            finalCards = threeOfAKind.get(0);

            playerHand.HandScore = getHandValue(finalCards, PokerHand.THREE_OF_A_KIND);
            playerHand.Hand = PokerHand.THREE_OF_A_KIND;
            return;
        }

        // Two Pairs
        if (sameRankCards.stream().filter(l -> l.size() == 2).count() >= 2) {
            List<List<Card>> pairs = sameRankCards.stream().filter(l -> l.size() == 2).sorted(Comparator.comparing(l -> l.get(0).Rank.ordinal())).collect(Collectors.toList());

            finalCards = pairs.get(0);
            finalCards.addAll(pairs.get(1));

            playerHand.HandScore = getHandValue(finalCards, PokerHand.TWO_PAIR);
            playerHand.Hand = PokerHand.TWO_PAIR;
            return;
        }

        // Pair
        if (sameRankCards.stream().anyMatch(l -> l.size() == 2)) {
            List<List<Card>> pairs = sameRankCards.stream().filter(l -> l.size() == 2).sorted(Comparator.comparing(l -> l.get(0).Rank.ordinal())).collect(Collectors.toList());

            finalCards = pairs.get(0);

            playerHand.HandScore = getHandValue(finalCards, PokerHand.PAIR);
            playerHand.Hand = PokerHand.PAIR;
            return;
        }

        //High Card
        finalCards = new ArrayList<>();
        finalCards.add(allCards.get(0));

        playerHand.HandScore = getHandValue(finalCards, PokerHand.HIGH_CARD);
        playerHand.Hand = PokerHand.HIGH_CARD;
    }

    private List<List<Card>> getSameTypeCards(List<Card> cardList) {
        List<List<Card>> sameCards = new ArrayList<>();

        for (CardType type : CardType.values()) {
            sameCards.add(cardList.stream().filter(card -> card.Type.equals(type)).collect(Collectors.toList()));
        }
        sameCards.sort(Comparator.comparing(List::size));
        return sameCards;
    }

    private List<List<Card>> getSameRankCards(List<Card> cardList) {
        List<List<Card>> sameCards = new ArrayList<>();

        for (CardRank rank : CardRank.values()) {
            sameCards.add(cardList.stream().filter(card -> card.Rank.equals(rank)).collect(Collectors.toList()));
        }
        sameCards.sort(Comparator.comparing(List::size));
        return sameCards;
    }

    private List<Card> getHighestStraight(List<Card> cards) {
        cards.sort(Comparator.comparing(c -> c.Rank.ordinal()));

        List<Card> finalCards = new ArrayList<>();

        int inRow = 0;
        Card ACE = cards.stream().filter(c -> c.Rank.equals(CardRank.ACE)).findFirst().orElse(null);
        if (ACE != null) {
            inRow = 1;
            finalCards.add(ACE);
        }

        for (Card card : cards) {
            if (finalCards.size() == 0) {
                finalCards.add(card);
                continue;
            }
            Card lastCard = finalCards.get(finalCards.size() - 1);

            if (inRow == 5 && card.Value.equals(lastCard.Value + 1)) {
                finalCards.remove(0);
                finalCards.add(card);
            } else if (inRow != 5 && ((card.Rank.equals(CardRank.TWO) && lastCard.Rank.equals(CardRank.ACE)) || card.Value.equals(lastCard.Value + 1))) {
                finalCards.add(card);
                inRow++;
            } else if (inRow != 5 && !card.Value.equals(lastCard.Value)) {
                finalCards.clear();
                finalCards.add(card);
                inRow = 1;
            }
        }

        if (inRow == 5) return finalCards;
        else return null;
    }

    private int getHandValue(List<Card> cards, PokerHand hand) {
        int value = 0;

        for (Card card : cards) value += card.Value;

        if (hand.equals(PokerHand.STRAIGHT) || hand.equals(PokerHand.STRAIGHT_FLUSH)) {
            if (cards.stream().anyMatch(c -> c.Rank.equals(CardRank.ACE)) && cards.stream().anyMatch(c -> c.Rank.equals(CardRank.TWO)))
                value -= 13;
        }
        return value;
    }
}
