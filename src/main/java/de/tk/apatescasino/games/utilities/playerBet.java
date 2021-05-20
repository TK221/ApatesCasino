package de.tk.apatescasino.games.utilities;

public class playerBet {
    private Integer money;
    private Integer stake;

    public playerBet(int money, int stake) {
        this.money = money;
        this.stake = stake;
    }

    public Integer getMoney() {
        return money;
    }

    public void setMoney(int amount) {
        money = amount;
    }

    public void addMoney(int amount) {
        money += amount;
    }

    public boolean removeMoney(int amount) {
        if (money >= amount) {
            money -= amount;
            return true;
        } else return false;
    }


    public Integer getStake() {
        return stake;
    }

    public void resetStake() {
        stake = 0;
    }

    private void addMoneyToStake(int amount) {
        stake += amount;
    }

    private boolean removeFromStake(int amount) {
        if (stake >= amount) {
            stake -= amount;
            return true;
        } else return false;
    }


    public boolean transferMoneyToStake(int amount) {
        if (removeMoney(amount)) {
            addMoneyToStake(amount);
            return true;
        } else return false;
    }

    public boolean transferStakeToMoney(int amount) {
        if (removeFromStake(amount)) {
            addMoney(amount);
            return true;
        } else return false;
    }

}
