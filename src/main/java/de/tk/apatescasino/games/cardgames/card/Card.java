package de.tk.apatescasino.games.cardgames.card;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;


public class Card {

    public CardColor color;
    public CardType type;
    public CardRank rank;
    public Integer value;


    public Card(CardColor color, CardType type, CardRank rank, Integer value) {
        this.color = color;
        this.type = type;
        this.rank = rank;
        this.value = value;
    }

    public static ItemStack getCardItem(Card card) {
        ItemStack cardItem = new ItemStack(Material.PAPER, 1);
        ItemMeta itemMeta = cardItem.getItemMeta();
        if (itemMeta == null) return null;

        String cardName = "";

        if (card.color == CardColor.RED) cardName += ChatColor.RED;
        else cardName += ChatColor.DARK_GRAY;

        cardName += getTypeSymbol(card.type) + " " + getGermanRank(card.rank) + " " + getTypeSymbol(card.type);

        itemMeta.setDisplayName(cardName);
        cardItem.setItemMeta(itemMeta);

        return cardItem;
    }

    public static String getTextCard(Card card) {
        return getTypeSymbol(card.type) + " " + getCardColor(card.type) + getGermanRank(card.rank) + " " + getTypeSymbol(card.type) + ChatColor.WHITE;
    }

    public static String getCardsAsText(List<Card> cards) {
        if (cards.size() == 0) return "";
        StringBuilder cardMessage = new StringBuilder(ChatColor.WHITE.toString());

        for (Card card : cards) {
            cardMessage.append(" | ").append(Card.getTextCard(card));
        }
        cardMessage.append(" |");

        return cardMessage.toString();
    }

    public static String getCardColor(CardType type) {
        if (type == CardType.CLUB || type == CardType.SPADE) return ChatColor.DARK_GRAY.toString();
        else return ChatColor.DARK_RED.toString();
    }

    public static String getTypeSymbol(CardType type) {
        String symbol = "";
        switch (type) {
            case CLUB:
                symbol += ChatColor.DARK_GRAY + "♣";
                break;
            case SPADE:
                symbol += ChatColor.BLACK + "♠";
                break;
            case HEART:
                symbol += ChatColor.RED + "♥";
                break;
            case DIAMOND:
                symbol += ChatColor.DARK_RED + "♦";
                break;
        }
        return symbol += ChatColor.WHITE;
    }

    public static String getGermanRank(CardRank rank) {
        switch (rank) {
            case TWO:
                return "2";
            case THREE:
                return "3";
            case FOUR:
                return "4";
            case FIVE:
                return "5";
            case SIX:
                return "6";
            case SEVEN:
                return "7";
            case EIGHT:
                return "8";
            case NINE:
                return "9";
            case TEN:
                return "10";
            case JACK:
                return "Bube";
            case QUEEN:
                return "Dame";
            case KING:
                return "Koenig";
            case ACE:
                return "Ass";
        }
        return null;
    }
}
