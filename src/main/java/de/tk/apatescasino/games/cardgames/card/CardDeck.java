package de.tk.apatescasino.games.cardgames.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CardDeck {

    public final List<Card> Cards = new ArrayList<>();


    public void InitStandardDeck() {
        for (CardType type : CardType.values()) {
            CardColor color = (type == CardType.CLUB || type == CardType.SPADE) ? CardColor.BLACK : CardColor.RED;


            for (CardRank rank : CardRank.values()) {
                Cards.add(new Card(color, type, rank, (rank.ordinal() + 2)));
            }
        }
    }

    public void ShuffleDeck()
    {
        Collections.shuffle(Cards);
    }

    public Card pickFirst() {
        Card card = Cards.stream().findFirst().orElse(null);
        Cards.remove(card);
        return card;
    }
}
