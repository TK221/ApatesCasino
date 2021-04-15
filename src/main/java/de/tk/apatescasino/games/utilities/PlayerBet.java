package de.tk.apatescasino.games.utilities;

public class PlayerBet {
    private Integer money;
    private Integer stake;

    public PlayerBet(int money, int stake) {
        this.money = money;
        this.stake = stake;
    }

    public Integer GetMoney() { return money; }

    public void SetMoney(int amount) { money = amount; }

    public void AddMoney(int amount) { money += amount; }

    private boolean removeMoney(int amount) {
        if (money >= amount) {
            money -= amount;
            return true;
        } else return false;
    }


    public Integer GetStake() { return stake; }

    public void ResetStake() { stake = 0; }

    private void addMoneyToStake(int amount) { stake += amount; }

    private boolean removeFromStake(int amount) {
        if (stake >= amount) {
            stake -= amount;
            return true;
        } else return false;
    }


    public boolean TransferMoneyToStake(int amount) {
        if (removeMoney(amount)) {
            addMoneyToStake(amount);
            return true;
        } else return  false;
    }
    public boolean TransferStakeToMoney(int amount) {
        if (removeFromStake(amount)) {
            AddMoney(amount);
            return true;
        } else return false;
    }

}
