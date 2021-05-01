package de.tk.apatescasino.games.cardgames.poker;

import de.tk.apatescasino.games.utilities.PlayerBet;
import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PokerBetHandler {
    public final int MinMoney;
    public final int SmallBlind;
    public final int BigBlind;
    public Integer CurrentMinBet;
    public Integer Pot;
    public Map<Integer, Integer> sidePots;

    // Presets of betting amounts
    public final Integer[] BetAmounts;

    public Integer SmallBlindPlayer;
    public Integer BigBlindPlayer;


    public PokerBetHandler(int minMoney, int smallBlind, int bigBlind) {
        this.MinMoney = minMoney;
        this.SmallBlind = smallBlind;
        this.BigBlind = bigBlind;

        this.SmallBlindPlayer = 0;
        this.BigBlindPlayer = 0;

        BetAmounts = new Integer[]{5, 10, 50, 100, 500};
    }

    public boolean PlayerBetMoney(PlayerBet bet, int amount) {
        if (bet == null) return false;

        if (bet.TransferMoneyToStake(amount)) {
            Pot += amount;
            if (CurrentMinBet < bet.GetStake()) CurrentMinBet = bet.GetStake();
            return true;
        } else return false;

    }

    public void DistributeMoney(List<List<PokerPlayerProperties>> playerWinOrder) {
        int endPot = Pot;

        for (List<PokerPlayerProperties> playerList : playerWinOrder) {
            if (endPot <= 0) return;

            List<PokerPlayerProperties> highestStakePlayerProperties = playerList.stream().sorted(Comparator.comparing(p -> p.bet.GetStake())).collect(Collectors.toList());
            Collections.reverse(highestStakePlayerProperties);

            int highestStakePlayerNumber = highestStakePlayerProperties.get(0).playerNumber;
            int maxPot = sidePots.getOrDefault(highestStakePlayerNumber, endPot);

            int totalStake = 0;
            for (PokerPlayerProperties playerProperties : highestStakePlayerProperties)
                totalStake += playerProperties.bet.GetStake();

            for (PokerPlayerProperties playerProperties : playerList) {
                float percentage = (float) playerProperties.bet.GetStake() / totalStake;
                float money = endPot < maxPot ? endPot * percentage : maxPot * percentage;

                playerProperties.bet.AddMoney((int) money);

                System.out.println(playerProperties.Player.getDisplayName() + ": " + money);
            }

            endPot -= maxPot;
            System.out.println("Pot: " + endPot);
        }
    }

    public static String getBalanceDifferenceText(int currMoney, int oldMoney) {
        int difference = currMoney - oldMoney;

        String message = "";
        message += difference < 0 ? ChatColor.RED : ChatColor.GREEN;
        message += difference + " T" + ChatColor.WHITE;

        return message;
    }
}
