package de.tk.apatescasino;

import de.tk.apatescasino.bank.BankAccountHandler;
import de.tk.apatescasino.games.ChatMessageHandler;
import de.tk.apatescasino.games.Game;
import de.tk.apatescasino.games.GameListener;
import de.tk.apatescasino.games.cardgames.blackjack.BlackJackListener;
import de.tk.apatescasino.games.cardgames.poker.PokerListener;
import de.tk.apatescasino.games.commands.CasinoCommand;
import de.tk.apatescasino.games.config.GameConfigManager;
import de.tk.apatescasino.games.lobby.LobbyManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;


public final class ApatesCasino extends JavaPlugin {


    private static ApatesCasino instance;

    private static Economy econ = null;

    private final LobbyManager lobbyManager = new LobbyManager();
    private final GameConfigManager gameConfigManager = new GameConfigManager(lobbyManager);
    private static BankAccountHandler bankAccountHandler;
    private final static ChatMessageHandler chatMessageHandler = new ChatMessageHandler();

    @Override
    public void onEnable() {
        // Plugin startup login
        getDataFolder().mkdirs();

        if (!setupEconomy()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        instance = this;
        bankAccountHandler = new BankAccountHandler(econ);

        getServer().getPluginManager().registerEvents(new GameListener(lobbyManager, gameConfigManager), this);
        getServer().getPluginManager().registerEvents(new PokerListener(lobbyManager), this);
        getServer().getPluginManager().registerEvents(new BlackJackListener(lobbyManager), this);

        Objects.requireNonNull(this.getCommand("casino")).setExecutor(new CasinoCommand(lobbyManager, gameConfigManager));

        gameConfigManager.CreateAllGames();
    }

    @Override
    public void onDisable() {
        for (Game game : lobbyManager.GetAllGames()) game.CancelGame();
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

    public static BankAccountHandler getBankAccountHandler() {
        return bankAccountHandler;
    }

    public static ChatMessageHandler GetChatMessageHandler() {
        return chatMessageHandler;
    }
}
