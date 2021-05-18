package de.tk.apatescasino.games.cardgames.poker;

import de.tk.apatescasino.games.cardgames.card.Card;
import de.tk.apatescasino.games.cardgames.card.CardDeck;
import de.tk.apatescasino.games.cardgames.card.CardRank;
import de.tk.apatescasino.games.cardgames.card.CardType;
import org.bukkit.ChatColor;

import java.util.*;
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
    public Card firstCard;
    public Card secondCard;
    public PokerHand hand;
    public Integer handScore;
}

public class PokerCardHandler {

    // Current card deck
    public CardDeck deck;
    // Cards which are showed on the table
    public List<Card> showedCards;


    public void initGameCardHandler() {
        // Initialize card deck and shuffle it
        deck = new CardDeck();
        deck.InitStandardDeck();
        deck.ShuffleDeck();

        // Show three cards to the table
        showedCards = new ArrayList<>();
    }

    public void initPlayerPokerCards(PlayerPokerHand hand) {
        hand.firstCard = deck.pickFirst();
        hand.secondCard = deck.pickFirst();
    }

    public List<List<PokerPlayerProperties>> getWinners(List<PokerPlayerProperties> players) {
        List<List<PokerPlayerProperties>> playerWinOrder = new ArrayList<>();

        if (players.size() == 0) return playerWinOrder;

        for (PokerPlayerProperties player : players) calculatePlayerHand(player.hand);

        players.sort(Comparator.comparing(p -> p.hand.hand.ordinal()));
        Collections.reverse(players);

        List<PokerHand> pokerHands = Arrays.asList(PokerHand.values());
        Collections.reverse(pokerHands);
        for (PokerHand pokerHand : pokerHands) {
            List<PokerPlayerProperties> handPlayerList = new ArrayList<>();

            for (PokerPlayerProperties playerProperties : players)
                if (playerProperties.hand.hand.equals(pokerHand)) handPlayerList.add(playerProperties);

            List<List<PokerPlayerProperties>> groupedScorePlayerList = new ArrayList<>(handPlayerList.stream().collect(Collectors.groupingBy(p -> p.hand.handScore)).values());
            for (List<PokerPlayerProperties> scorePlayerPropertiesList : groupedScorePlayerList) {

                List<CardRank> cardRanks = Arrays.asList(CardRank.values());
                Collections.reverse(cardRanks);
                for (CardRank cardRank : cardRanks) {

                    List<PokerPlayerProperties> cardPlayerProperties = scorePlayerPropertiesList.stream().filter(p -> p.hand.firstCard.rank.equals(cardRank) || p.hand.secondCard.rank.equals(cardRank)).collect(Collectors.toList());

                    for (int i = 0; i < cardPlayerProperties.size(); i++) {
                        PokerPlayerProperties playerProperties = cardPlayerProperties.get(i);
                        if (playerProperties == null) continue;

                        for (List<PokerPlayerProperties> playerList : playerWinOrder) {
                            if (playerList.contains(playerProperties)) {
                                cardPlayerProperties.remove(playerProperties);
                            }
                        }
                    }

                    if (cardPlayerProperties.size() > 0) playerWinOrder.add(cardPlayerProperties);
                }
            }
        }

        return playerWinOrder;
    }

    private void calculatePlayerHand(PlayerPokerHand playerHand) {
        if (playerHand == null) return;

        List<Card> allCards = new ArrayList<>(showedCards);
        allCards.add(playerHand.firstCard);
        allCards.add(playerHand.secondCard);
        allCards.sort(Comparator.comparing(c -> c.rank.ordinal()));

        List<List<Card>> sameTypeCards = getSameTypeCards(allCards);
        List<List<Card>> sameRankCards = getSameRankCards(allCards);

        List<Card> finalCards;

        // Royal FLush
        if (sameTypeCards.stream().anyMatch(l -> l.size() >= 5)) {
            List<Card> cards = sameTypeCards.stream().filter(l -> l.size() >= 5).findFirst().orElse(new ArrayList<>());
            cards.sort(Comparator.comparing(c -> c.rank.ordinal()));

            finalCards = new ArrayList<>();
            int index = CardRank.TEN.ordinal();

            for (Card card : cards) {
                if (card.rank.ordinal() == index) {
                    index++;
                    finalCards.add(card);
                }
            }
            if (index > CardRank.ACE.ordinal()) {
                playerHand.handScore = getHandValue(finalCards, PokerHand.ROYAL_FLUSH);
                playerHand.hand = PokerHand.ROYAL_FLUSH;
                return;
            }
        }

        // Straight Flush
        if (sameTypeCards.stream().anyMatch(l -> l.size() >= 5)) {
            List<Card> cards = sameTypeCards.stream().filter(l -> l.size() >= 5).findFirst().orElse(new ArrayList<>());

            finalCards = getHighestStraight(cards);
            if (finalCards != null) {
                playerHand.handScore = getHandValue(finalCards, PokerHand.STRAIGHT_FLUSH);
                playerHand.hand = PokerHand.STRAIGHT_FLUSH;
                return;
            }
        }

        // Four of a kind
        if (sameRankCards.stream().anyMatch(l -> l.size() == 4)) {
            List<Card> cards = sameRankCards.stream().filter(l -> l.size() == 4).findFirst().orElse(new ArrayList<>());

            playerHand.handScore = getHandValue(cards, PokerHand.FOUR_OF_A_KIND);
            playerHand.hand = PokerHand.FOUR_OF_A_KIND;
            return;
        }

        // Full House
        if (sameRankCards.stream().anyMatch(l -> l.size() == 3) && sameRankCards.stream().anyMatch(l -> l.size() == 2)) {
            List<List<Card>> threeOfAKind = sameRankCards.stream().filter(l -> l.size() == 3).collect(Collectors.toList());
            List<List<Card>> pairs = sameRankCards.stream().filter(l -> l.size() == 2).collect(Collectors.toList());

            threeOfAKind.sort(Comparator.comparing(l -> l.get(0).rank.ordinal()));
            pairs.sort(Comparator.comparing(l -> l.get(0).rank.ordinal()));

            finalCards = threeOfAKind.get(0);
            finalCards.addAll(pairs.get(0));

            playerHand.handScore = getHandValue(finalCards, PokerHand.FULL_HOUSE);
            playerHand.hand = PokerHand.FULL_HOUSE;
            return;
        }

        // Flush
        if (sameTypeCards.stream().anyMatch(l -> l.size() >= 5)) {
            List<Card> cards = sameTypeCards.stream().filter(l -> l.size() >= 5).findFirst().orElse(new ArrayList<>());
            cards.sort(Comparator.comparing(c -> c.rank.ordinal()));

            if (cards.size() > 5) {
                cards.subList(0, (cards.size() - 5)).clear();
            }

            playerHand.handScore = getHandValue(cards, PokerHand.FLUSH);
            playerHand.hand = PokerHand.FLUSH;
            return;
        }

        // Straight
        if (allCards.size() >= 5) {
            List<Card> cards = new ArrayList<>(allCards);

            finalCards = getHighestStraight(cards);

            if (finalCards != null) {
                playerHand.handScore = getHandValue(cards, PokerHand.STRAIGHT);
                playerHand.hand = PokerHand.STRAIGHT;
                return;
            }
        }

        // Three of a kind
        if (sameRankCards.stream().anyMatch(l -> l.size() == 3)) {
            List<List<Card>> threeOfAKind = sameRankCards.stream().filter(l -> l.size() == 3).sorted(Comparator.comparing(l -> l.get(0).rank.ordinal())).collect(Collectors.toList());

            finalCards = threeOfAKind.get(0);

            playerHand.handScore = getHandValue(finalCards, PokerHand.THREE_OF_A_KIND);
            playerHand.hand = PokerHand.THREE_OF_A_KIND;
            return;
        }

        // Two Pairs
        if (sameRankCards.stream().filter(l -> l.size() == 2).count() >= 2) {
            List<List<Card>> pairs = sameRankCards.stream().filter(l -> l.size() == 2).sorted(Comparator.comparing(l -> l.get(0).rank.ordinal())).collect(Collectors.toList());

            finalCards = pairs.get(0);
            finalCards.addAll(pairs.get(1));

            playerHand.handScore = getHandValue(finalCards, PokerHand.TWO_PAIR);
            playerHand.hand = PokerHand.TWO_PAIR;
            return;
        }

        // Pair
        if (sameRankCards.stream().anyMatch(l -> l.size() == 2)) {
            List<List<Card>> pairs = sameRankCards.stream().filter(l -> l.size() == 2).sorted(Comparator.comparing(l -> l.get(0).rank.ordinal())).collect(Collectors.toList());

            finalCards = pairs.get(0);

            playerHand.handScore = getHandValue(finalCards, PokerHand.PAIR);
            playerHand.hand = PokerHand.PAIR;
            return;
        }

        //High Card
        finalCards = new ArrayList<>();
        finalCards.add(allCards.get(0));

        playerHand.handScore = getHandValue(finalCards, PokerHand.HIGH_CARD);
        playerHand.hand = PokerHand.HIGH_CARD;
    }

    private List<List<Card>> getSameTypeCards(List<Card> cardList) {
        List<List<Card>> sameCards = new ArrayList<>();

        for (CardType type : CardType.values()) {
            sameCards.add(cardList.stream().filter(card -> card.type.equals(type)).collect(Collectors.toList()));
        }
        sameCards.sort(Comparator.comparing(List::size));
        return sameCards;
    }

    private List<List<Card>> getSameRankCards(List<Card> cardList) {
        List<List<Card>> sameCards = new ArrayList<>();

        for (CardRank rank : CardRank.values()) {
            sameCards.add(cardList.stream().filter(card -> card.rank.equals(rank)).collect(Collectors.toList()));
        }
        sameCards.sort(Comparator.comparing(List::size));
        return sameCards;
    }

    private List<Card> getHighestStraight(List<Card> cards) {
        cards.sort(Comparator.comparing(c -> c.rank.ordinal()));

        List<Card> finalCards = new ArrayList<>();

        int inRow = 0;
        Card ACE = cards.stream().filter(c -> c.rank.equals(CardRank.ACE)).findFirst().orElse(null);
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

            if (inRow == 5 && card.value.equals(lastCard.value + 1)) {
                finalCards.remove(0);
                finalCards.add(card);
            } else if (inRow != 5 && ((card.rank.equals(CardRank.TWO) && lastCard.rank.equals(CardRank.ACE)) || card.value.equals(lastCard.value + 1))) {
                finalCards.add(card);
                inRow++;
            } else if (inRow != 5 && !card.value.equals(lastCard.value)) {
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

        for (Card card : cards) value += card.value;

        if (hand.equals(PokerHand.STRAIGHT) || hand.equals(PokerHand.STRAIGHT_FLUSH)) {
            if (cards.stream().anyMatch(c -> c.rank.equals(CardRank.ACE)) && cards.stream().anyMatch(c -> c.rank.equals(CardRank.TWO)))
                value -= 13;
        }
        return value;
    }

    public static String getHandText(PokerHand hand) {
        String message = "";

        switch (hand) {
            case HIGH_CARD:
                message += ChatColor.DARK_AQUA + "High Card";
                break;
            case PAIR:
                message += ChatColor.DARK_AQUA + "Pair";
                break;
            case TWO_PAIR:
                message += ChatColor.AQUA + "Two Pair";
                break;
            case THREE_OF_A_KIND:
                message += ChatColor.AQUA + "Three of a Kind";
                break;
            case STRAIGHT:
                message += ChatColor.GREEN + "Straight";
                break;
            case FLUSH:
                message += ChatColor.GREEN + "Flush";
                break;
            case FULL_HOUSE:
                message += ChatColor.BLUE + "Full House";
                break;
            case FOUR_OF_A_KIND:
                message += ChatColor.LIGHT_PURPLE + "Four od a kind";
                break;
            case STRAIGHT_FLUSH:
                message += ChatColor.DARK_PURPLE + "Straight flush";
                break;
            case ROYAL_FLUSH:
                message += ChatColor.DARK_RED + "Royal flush";
                break;
        }

        return message + ChatColor.WHITE;
    }

    public static String getCardHandText(PlayerPokerHand pokerHand) {
        return ChatColor.WHITE + "| " + Card.getTextCard(pokerHand.firstCard) + " | " + Card.getTextCard(pokerHand.secondCard) + " | - " + getHandText(pokerHand.hand);
    }
}
