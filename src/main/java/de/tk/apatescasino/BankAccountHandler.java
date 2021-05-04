package de.tk.apatescasino;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BankAccountHandler {

    private static final String CASINO_BANK_ACCOUNT = "Casino Apate";

    private final Economy econ;


    public BankAccountHandler(Economy economy) {
        this.econ = economy;

        if (!econ.getBanks().contains(CASINO_BANK_ACCOUNT)) {
            econ.createBank(CASINO_BANK_ACCOUNT, Bukkit.getOfflinePlayer(UUID.fromString("755dfd18-d3fa-4f7a-a290-9c6db31f2d77")));
        }
    }


    public void transferToPlayer(Player player, int amount) {
        econ.depositPlayer(player, amount);
        econ.bankWithdraw(CASINO_BANK_ACCOUNT, amount);
    }

    public void transferToCasino(Player player, int amount) {
        econ.withdrawPlayer(player, amount);
        econ.bankDeposit(CASINO_BANK_ACCOUNT, amount);
    }

    public int getBalance() {
        return (int) econ.bankBalance(CASINO_BANK_ACCOUNT).balance;
    }

    public boolean hasEnoughMoney(int amount) {
        return econ.bankHas(CASINO_BANK_ACCOUNT, amount).type.equals(EconomyResponse.ResponseType.SUCCESS);
    }
}
