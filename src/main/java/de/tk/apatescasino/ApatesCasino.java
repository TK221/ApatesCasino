package de.tk.apatescasino;

import de.tk.apatescasino.games.LobbyManager;
import de.tk.apatescasino.games.cardgames.poker.PokerListener;
import de.tk.apatescasino.games.commands.CasinoCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;


public final class ApatesCasino extends JavaPlugin {

    private static ApatesCasino instance;

    public LobbyManager lobbyManager = new LobbyManager();

    @Override
    public void onEnable() {
        // Plugin startup login

        instance = this;

        getServer().getPluginManager().registerEvents(new PokerListener(lobbyManager), this);

        Objects.requireNonNull(this.getCommand("casino")).setExecutor(new CasinoCommand(lobbyManager));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        lobbyManager.ActiveGames.forEach((key, value) -> value.CancelGame());
    }

    public static ApatesCasino getInstance() {
        return instance;
    }
}
