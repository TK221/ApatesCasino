package de.tk.apatescasino.games.cardgames.poker;

import de.tk.apatescasino.games.utilities.playerBet;
import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PokerBetHandler {
    public final int minMoney;
    public final int smallBlind;
    public final int bigBlind;
    public final double fee;
    public Integer currentMinBet;
    public Integer pot;
    public Map<Integer, Integer> sidePots;

    // Presets of betting amounts
    public final Integer[] betAmounts;

    public Integer smallBlindPlayer;
    public Integer bigBlindPlayer;


    public PokerBetHandler(int minMoney, int smallBlind, int bigBlind, double fee) {
        this.minMoney = minMoney;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.fee = fee;

        this.smallBlindPlayer = 0;
        this.bigBlindPlayer = 0;

        betAmounts = new Integer[]{5, 10, 50, 100, 500};
    }

    public boolean playerBetMoney(playerBet bet, int amount) {
        if (bet == null) return false;

        if (bet.transferMoneyToStake(amount)) {
            pot += amount;
            if (currentMinBet < bet.getStake()) currentMinBet = bet.getStake();
            return true;
        } else return false;

    }

    public void distributeMoney(List<List<PokerPlayerProperties>> playerWinOrder) {
        int endPot = pot;

        for (List<PokerPlayerProperties> playerList : playerWinOrder) {
            if (endPot <= 0) return;

            List<PokerPlayerProperties> highestStakePlayerProperties = playerList.stream().sorted(Comparator.comparing(p -> p.bet.getStake())).collect(Collectors.toList());
            Collections.reverse(highestStakePlayerProperties);

            int highestStakePlayerNumber = highestStakePlayerProperties.get(0).playerNumber;
            int maxPot = sidePots.getOrDefault(highestStakePlayerNumber, endPot);

            int totalStake = 0;
            for (PokerPlayerProperties playerProperties : highestStakePlayerProperties)
                totalStake += playerProperties.bet.getStake();

            for (PokerPlayerProperties playerProperties : playerList) {
                float percentage = (float) playerProperties.bet.getStake() / totalStake;
                float money = endPot < maxPot ? endPot * percentage : maxPot * percentage;

                playerProperties.bet.addMoney((int) money);

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
