package de.tk.apatescasino.games.cardgames.poker;

import de.tk.apatescasino.games.utilities.PlayerBet;

public class PokerBetHandler {
    public final int MinMoney;
    public final int SmallBlind;
    public final int BigBlind;
    public Integer CurrentMinBet;
    public Integer Pot;

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
            if (CurrentMinBet < bet.GetStake()) CurrentMinBet = bet.GetStake();
            return true;
        }
        else return false;

    }
}
