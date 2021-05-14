package de.tk.apatescasino.bank;

public class Transaction {
    public boolean Profit;
    public String PlayerName;
    public int Amount;
    public String DateTime;
    public int Bank;

    public Transaction(boolean profit, String playerName, int amount, String dateTime, int bank) {
        Profit = profit;
        PlayerName = playerName;
        Amount = amount;
        DateTime = dateTime;
        Bank = bank;
    }
}
