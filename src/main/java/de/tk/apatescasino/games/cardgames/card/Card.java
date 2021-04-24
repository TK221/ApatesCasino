package de.tk.apatescasino.games.cardgames.card;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class Card {

    public CardColor Color;
    public CardType Type;
    public CardRank Rank;
    public Integer Value;


    public Card(CardColor color, CardType type, CardRank rank, Integer value) {
        this.Color = color;
        this.Type = type;
        this.Rank = rank;
        this.Value = value;
    }

    public static ItemStack getCardItem(Card card) {
        ItemStack cardItem = new ItemStack(Material.PAPER, 1);
        ItemMeta itemMeta = cardItem.getItemMeta();
        if (itemMeta == null) return null;

        String cardName = "";

        if (card.Color == CardColor.RED) cardName += ChatColor.RED;
        else cardName += ChatColor.DARK_GRAY;

        cardName += GetTypeSymbol(card.Type) + " " + GetGermanRank(card.Rank) + " " + GetTypeSymbol(card.Type);

        itemMeta.setDisplayName(cardName);
        cardItem.setItemMeta(itemMeta);

        return cardItem;
    }

    public static String GetTextCard(Card card) {
        return GetTypeSymbol(card.Type) + " " + GetCardColor(card.Type) + GetGermanRank(card.Rank) + " " + GetTypeSymbol(card.Type) + ChatColor.WHITE;
    }

    public static String GetCardColor(CardType type) {
        if (type == CardType.CLUB || type == CardType.SPADE) return ChatColor.DARK_GRAY.toString();
        else return ChatColor.DARK_RED.toString();
    }

    public static String GetTypeSymbol(CardType type) {
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

    public static String GetGermanRank(CardRank rank) {
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
