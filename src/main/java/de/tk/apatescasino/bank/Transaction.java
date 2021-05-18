package de.tk.apatescasino.bank;

public class Transaction {
    public boolean profit;
    public String playerName;
    public int amount;
    public String dateTime;
    public int bank;

    public Transaction(boolean profit, String playerName, int amount, String dateTime, int bank) {
        this.profit = profit;
        this.playerName = playerName;
        this.amount = amount;
        this.dateTime = dateTime;
        this.bank = bank;
    }
}
