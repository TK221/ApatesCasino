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


    public Card(CardColor color, CardType type, CardRank rank, Integer value ) {
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

        switch (card.Type) {
            case CLUB:
                cardName += "Kreuz";
                break;
            case SPADE:
                cardName += "Pik";
                break;
            case HEART:
                cardName += "Herz";
                break;
            case DIAMOND:
                cardName += "Karo";
                break;
        }
        cardName += " ";

        switch (card.Rank) {
            case ACE:
                cardName += "Ass";
                break;
            case TWO:
                cardName += "Zwei";
                break;
            case THREE:
                cardName += "Drei";
                break;
            case FOUR:
                cardName += "Vier";
                break;
            case FIVE:
                cardName += "Fuenf";
                break;
            case SIX:
                cardName += "Sechs";
                break;
            case SEVEN:
                cardName += "Sieben";
                break;
            case EIGHT:
                cardName += "Acht";
                break;
            case NINE:
                cardName += "Neun";
                break;
            case TEN:
                cardName += "Zehn";
                break;
            case JACK:
                cardName += "Bube";
                break;
            case QUEEN:
                cardName += "Dame";
                break;
            case KING:
                cardName += "Koenig";
                break;
        }

        itemMeta.setDisplayName(cardName);
        cardItem.setItemMeta(itemMeta);

        return cardItem;
    }
}
