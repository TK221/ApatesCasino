package de.tk.apatescasino.bank;

import com.google.gson.reflect.TypeToken;
import de.tk.apatescasino.ApatesCasino;
import de.tk.apatescasino.ConfigManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BankAccountHandler {

    private static final String TRANSACTION_FOLDER = "Transactions/";
    private static final String CASINO_BANK_ACCOUNT = "CasinoApate";

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss", Locale.ROOT);

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
        addTransaction(new Transaction(false, player.getDisplayName(), amount, LocalDateTime.now().format(formatter), getBalance()));
    }

    public void transferToCasino(Player player, int amount) {
        econ.withdrawPlayer(player, amount);
        econ.bankDeposit(CASINO_BANK_ACCOUNT, amount);
        addTransaction(new Transaction(true, player.getDisplayName(), amount, LocalDateTime.now().format(formatter), getBalance()));
    }

    public void deposit(int amount) {
        econ.bankDeposit(CASINO_BANK_ACCOUNT, amount);
        addTransaction(new Transaction(true, "ANONYMOUS", amount, LocalDateTime.now().format(formatter), getBalance()));
    }

    public void withdraw(int amount) {
        econ.bankWithdraw(CASINO_BANK_ACCOUNT, amount);
        addTransaction(new Transaction(false, "ANONYMOUS", amount, LocalDateTime.now().format(formatter), getBalance()));
    }

    public int getBalance() {
        System.out.println(econ.bankBalance(CASINO_BANK_ACCOUNT).balance);
        return (int) econ.bankBalance(CASINO_BANK_ACCOUNT).balance;
    }

    public boolean hasEnoughMoney(int amount) {
        return econ.bankHas(CASINO_BANK_ACCOUNT, amount).type.equals(EconomyResponse.ResponseType.SUCCESS);
    }

    private void addTransaction(Transaction transaction) {
        try {
            String year = String.valueOf(LocalDateTime.now().getYear());
            String month = String.valueOf(LocalDateTime.now().getMonth());
            String day = String.valueOf(LocalDateTime.now().getDayOfMonth());
            String file = TRANSACTION_FOLDER + day + "_" + month + "_" + year;

            ConfigManager<List<Transaction>> configManager = new ConfigManager<>(file + ".json", ApatesCasino.getInstance());
            configManager.loadConfig(new TypeToken<List<Transaction>>() {
            }.getType());

            List<Transaction> transactions;
            if (configManager.getObject() == null) transactions = new ArrayList<>();
            else transactions = configManager.getObject();

            transactions.add(transaction);
            configManager.setObject(transactions);

            configManager.saveConfig();

        } catch (Exception e) {
            System.out.println("Error while add new transaction: " + e.getMessage());
        }
    }
}
