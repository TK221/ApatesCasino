package de.tk.apatescasino.bank;

import com.google.gson.reflect.TypeToken;
import de.tk.apatescasino.ApatesCasino;
import de.tk.apatescasino.ConfigManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    private final UUID ownerID;
    private final List<UUID> employees;

    private final int minMoney = 200;

    public BankAccountHandler(Economy economy) {
        this.econ = economy;

        ownerID = UUID.fromString("755dfd18-d3fa-4f7a-a290-9c6db31f2d77");
        employees = new ArrayList<>();
        employees.add(UUID.fromString("9cae7835-ca85-424a-b376-287345c4720d"));

        if (!econ.getBanks().contains(CASINO_BANK_ACCOUNT)) {
            econ.createBank(CASINO_BANK_ACCOUNT, Bukkit.getOfflinePlayer(ownerID));
        }
    }


    public void transferToPlayer(Player player, int amount) {
        if (amount > 0) {
            econ.depositPlayer(player, amount);
            econ.bankWithdraw(CASINO_BANK_ACCOUNT, amount);
            addTransaction(new Transaction(false, player.getDisplayName(), amount, LocalDateTime.now().format(formatter), getBalance()));
        }
    }

    public void transferToCasino(Player player, int amount) {
        if (amount > 0) {
            econ.withdrawPlayer(player, amount);
            econ.bankDeposit(CASINO_BANK_ACCOUNT, amount);
            addTransaction(new Transaction(true, player.getDisplayName(), amount, LocalDateTime.now().format(formatter), getBalance()));
        }
    }

    public void deposit(int amount) {
        if (amount > 0) {
            econ.bankDeposit(CASINO_BANK_ACCOUNT, amount);
            addTransaction(new Transaction(true, "ANONYMOUS", amount, LocalDateTime.now().format(formatter), getBalance()));
        }
    }

    public void withdraw(int amount) {
        if (amount > 0) {
            econ.bankWithdraw(CASINO_BANK_ACCOUNT, amount);
            addTransaction(new Transaction(false, "ANONYMOUS", amount, LocalDateTime.now().format(formatter), getBalance()));
        }
    }

    public int getBalance() {
        return (int) econ.bankBalance(CASINO_BANK_ACCOUNT).balance;
    }

    public boolean hasEnoughMoney() {
        return econ.bankHas(CASINO_BANK_ACCOUNT, minMoney).type.equals(EconomyResponse.ResponseType.SUCCESS);
    }

    public void writeBalance(Player player) {
        System.out.println(player.getUniqueId() + "  " + ownerID);

        if (!player.getUniqueId().equals(ownerID) || !employees.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Sie sind nicht der Besitzer des Casinos");
            return;
        }

        player.sendMessage("Das Casino besitzt: " + ChatColor.GOLD + getBalance() + " $");
    }

    public void writeLastTransactions(Player player) {
        if (!player.getUniqueId().equals(ownerID) || !employees.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Sie sind nicht der Besitzer des Casinos");
            return;
        }

        List<Transaction> transactions = loadTransactions();
        for (Transaction transaction : transactions) {
            String color = transaction.profit ? ChatColor.GREEN.toString() : ChatColor.GREEN.toString();

            String message = color + "[" + ChatColor.GOLD + transaction.amount + color + "] ";
            message += transaction.playerName + (transaction.profit ? " -> " : " <- ") + " Casino ( " + transaction.bank + " ) - " + transaction.dateTime;

            player.sendMessage(message);
        }
    }

    private void addTransaction(Transaction transaction) {
        List<Transaction> transactions = loadTransactions();
        transactions.add(transaction);
        saveTransactions(transactions);
    }

    private List<Transaction> loadTransactions() {
        List<Transaction> transactions = new ArrayList<>();

        try {
            ConfigManager<List<Transaction>> configManager = new ConfigManager<>(getTransactionFile() + ".json", ApatesCasino.getInstance());
            configManager.loadConfig(new TypeToken<List<Transaction>>() {
            }.getType());

            if (configManager.getObject() != null) transactions = configManager.getObject();
        } catch (Exception e) {
            System.out.println("Error while loading transactions " + e.getMessage());
        }

        return transactions;
    }

    private void saveTransactions(List<Transaction> transactions) {
        try {
            ConfigManager<List<Transaction>> configManager = new ConfigManager<>(getTransactionFile() + ".json", ApatesCasino.getInstance());
            configManager.loadConfig(new TypeToken<List<Transaction>>() {
            }.getType());

            configManager.setObject(transactions);
            configManager.saveConfig();
        } catch (Exception e) {
            System.out.println("Error while loading transactions " + e.getMessage());
        }
    }

    private static String getTransactionFile() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String month = String.valueOf(LocalDateTime.now().getMonth());
        String day = String.valueOf(LocalDateTime.now().getDayOfMonth());
        return TRANSACTION_FOLDER + day + "_" + month + "_" + year;
    }
}
