package de.tk.apatescasino;

import de.tk.apatescasino.games.GameListener;
import de.tk.apatescasino.games.LobbyManager;
import de.tk.apatescasino.games.cardgames.poker.PokerListener;
import de.tk.apatescasino.games.commands.CasinoCommand;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;


public final class ApatesCasino extends JavaPlugin {


    private static ApatesCasino instance;

    private final LobbyManager lobbyManager = new LobbyManager();
    private BankAccountHandler bankAccountHandler;
    private static Economy econ = null;

    @Override
    public void onEnable() {
        // Plugin startup login
        if (!setupEconomy()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        instance = this;
        bankAccountHandler = new BankAccountHandler(econ);

        getServer().getPluginManager().registerEvents(new PokerListener(lobbyManager), this);
        getServer().getPluginManager().registerEvents(new GameListener(lobbyManager), this);

        Objects.requireNonNull(this.getCommand("casino")).setExecutor(new CasinoCommand(lobbyManager, bankAccountHandler));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        lobbyManager.ActiveGames.forEach((key, value) -> value.CancelGame());
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }

    public static ApatesCasino getInstance() {
        return instance;
    }

    public static Economy getEconomy() {
        return econ;
    }
}
