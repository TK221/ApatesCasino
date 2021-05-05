package de.tk.apatescasino.games.cardgames.blackjack;

import de.tk.apatescasino.ApatesCasino;
import de.tk.apatescasino.BankAccountHandler;
import de.tk.apatescasino.games.Game;
import de.tk.apatescasino.games.ItemStackBuilder;
import de.tk.apatescasino.games.Lobby;
import de.tk.apatescasino.games.PlayerState;
import de.tk.apatescasino.games.cardgames.card.Card;
import de.tk.apatescasino.games.cardgames.card.CardDeck;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getServer;

enum BlackJackGameState {
    WAITING,
    PREPARING,
    ONGOING
}

public class BlackJack implements Game {

    private final int minPlayers;
    private final int maxPlayers;
    private final Location joinBlockPosition;

    private final ItemStack HIT_ITEM = ItemStackBuilder.createItemStack(Material.PINK_DYE, 1, (ChatColor.DARK_GREEN + "HIT"), new String[]{"Hole dir eine weitere Karte"});
    private final ItemStack STAND_ITEM = ItemStackBuilder.createItemStack(Material.LIME_DYE, 1, (ChatColor.DARK_BLUE + "STAND"), new String[]{"Beende deinen Zug"});
    private final ItemStack LEAVE_ITEM = ItemStackBuilder.createItemStack(Material.BARRIER, 1, (ChatColor.RED + "Leave"), new String[]{"Verlasse das Spiel"});

    private final Economy economy;
    private final BankAccountHandler bank;
    private final int minBet = 10;
    private final int maxBet = 1000;

    private BlackJackGameState gameState;
    private Integer currentPlayer;
    private BukkitTask turnTimer;
    private final Integer turnTime = 30;
    private final Integer preparingTime = 20;


    private final Map<UUID, BlackJackPlayer> playerMap;
    private final Lobby lobby;

    private final CardDeck cardDeck;
    private final List<Card> croupierCards;
    private Integer croupierCardsValue;

    public BlackJack(String id, int minPlayers, int maxPlayers, Location joinBlockPosition, BankAccountHandler bank) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.joinBlockPosition = joinBlockPosition;
        this.lobby = new Lobby(maxPlayers, minPlayers, id);
        this.bank = bank;

        playerMap = new HashMap<>();
        cardDeck = new CardDeck();
        economy = ApatesCasino.getEconomy();
        croupierCards = new ArrayList<>();
        croupierCardsValue = 0;
        gameState = BlackJackGameState.WAITING;

        bank.deposit(10000);
    }


    private void startPreparingTimer() {
        for (BlackJackPlayer player : playerMap.values())
            player.Player.sendMessage(ChatColor.BLUE + "Bitte schreibe deinen Einsatz den du setzen möchtest");

        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(ApatesCasino.getInstance(), () -> {

            for (BlackJackPlayer player : playerMap.values().stream().filter(p -> p.GetStake() == 0).collect(Collectors.toList())) {
                player.Player.sendMessage(ChatColor.RED + "Du hast leider keine Wette gesetzt");
                RemovePlayer(player.Player.getUniqueId());
            }

            if (playerMap.values().size() >= minPlayers) startNewGame();
            else gameState = BlackJackGameState.WAITING;

        }, preparingTime * 20L);
    }

    private void startTurnTimer() {
        int time = currentPlayer != -1 ? turnTime : 5;
        turnTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentPlayer != 0) {
                    BlackJackPlayer player = getPlayerByNumber(currentPlayer);
                    if (player != null) {
                        player.Player.sendMessage(ChatColor.RED + "Deine Zeit nun um, dein Zug ist beendet");
                        playerStand(player);
                    }
                }
                nextPlayer();
            }
        }.runTaskLater(ApatesCasino.getInstance(), (time * 20L));
    }

    private void startNewGame() {
        gameState = BlackJackGameState.ONGOING;

        croupierCards.clear();
        croupierCardsValue = 0;
        for (BlackJackPlayer player : playerMap.values()) player.ResetCards();

        // Set player states and write starting message
        for (BlackJackPlayer player : playerMap.values()) {
            player.state = BlackJackPlayerState.IN_GAME;
            player.Player.sendMessage(ChatColor.GREEN + "Das Spiel hat nun begonnen");
        }

        // Initialize Deck
        cardDeck.InitStandardDeck();
        cardDeck.ShuffleDeck();

        // Draw Cards
        for (BlackJackPlayer player : playerMap.values()) player.AddCard(cardDeck.pickFirst());
        croupierCards.add(cardDeck.pickFirst());
        for (BlackJackPlayer player : playerMap.values()) player.AddCard(cardDeck.pickFirst());

        currentPlayer = 0;
        nextPlayer();
    }

    private void nextPlayer() {
        if (turnTimer != null) turnTimer.cancel();
        // Get current player and set waiting properties
        BlackJackPlayer player = getPlayerByNumber(currentPlayer);
        if (player != null) {
            player.state = BlackJackPlayerState.IN_GAME;
            setWaitingBar(player);
        }

        //  Get next player and set the player turn or let the croupier do his turn
        if (currentPlayer != -1) currentPlayer = getNextPlayerNumber();
        if (currentPlayer > 0) {
            BlackJackPlayer nextPlayer = getPlayerByNumber(currentPlayer);
            playerTurn(nextPlayer);
        } else {
            currentPlayer = -1;
            croupierTurn();
        }
    }

    private void playerTurn(BlackJackPlayer player) {

        currentPlayer = player.PlayerNumber;
        player.state = BlackJackPlayerState.BETTING;
        setBettingBar(player);

        player.Player.sendMessage(ChatColor.GREEN + "Sie sind nun am Zug");

        sendMessage(player);
        startTurnTimer();
    }

    private void croupierTurn() {
        croupierCards.add(cardDeck.pickFirst());
        croupierCardsValue = BlackJackPlayer.getCalculatedCardsValue(croupierCards);

        if (croupierCardsValue < 17) {
            startTurnTimer();
        } else {
            endGame();
        }

        for (BlackJackPlayer player : playerMap.values()) {
            sendMessage(player);
        }
    }

    private void endGame() {

        for (BlackJackPlayer player : playerMap.values()) {
            if (player.GetCardsValue() < croupierCardsValue) {
                playerBust(player);
            } else if (player.GetCardsValue() > croupierCardsValue) {
                economy.depositPlayer(player.Player, player.GetStake());
                bank.transferToPlayer(player.Player, player.GetStake());
            } else {
                economy.depositPlayer(player.Player, player.GetStake());
            }

            player.ResetStake();
        }

        if (playerMap.values().size() >= minPlayers) {
            for (BlackJackPlayer player : playerMap.values())
                player.Player.sendMessage(ChatColor.GREEN + "Die runde ist beendet und startet erneut");

            gameState = BlackJackGameState.PREPARING;
            startPreparingTimer();
        } else {
            gameState = BlackJackGameState.WAITING;
        }
    }

    public void PlayerAction(UUID playerID, int slot) {
        if (!playerMap.containsKey(playerID)) return;
        BlackJackPlayer player = playerMap.get(playerID);

        if (player.state.equals(BlackJackPlayerState.IN_GAME)) {
            switch (slot) {
                case 8:
                    RemovePlayer(playerID);
                    break;
            }
        } else if (player.state.equals(BlackJackPlayerState.BETTING)) {
            switch (slot) {
                case 4:
                    playerHit(player);
                    break;
                case 5:
                    playerStand(player);
            }
        }
    }

    private void sendMessage(BlackJackPlayer player) {
        StringBuilder message = new StringBuilder("Croupier:\n| ");
        for (Card card : croupierCards) {
            message.append(Card.GetTextCard(card)).append(" | ");
        }
        message.append(" Wert: ").append(croupierCardsValue);

        message.append("\nPlayer:\n| ");
        for (Card card : player.GetCards()) {
            message.append(Card.GetTextCard(card)).append(" | ");
        }
        message.append(" Wert: ").append(player.GetCardsValue());
        player.Player.sendMessage(message.toString());
    }

    private void playerHit(BlackJackPlayer player) {
        player.AddCard(cardDeck.pickFirst());

        if (player.GetCardsValue() > 21) {
            playerBust(player);
            nextPlayer();
        }

        sendMessage(player);
    }

    private void playerStand(BlackJackPlayer player) {
        nextPlayer();
    }

    private void playerBust(BlackJackPlayer player) {

        // Deposit stake of the player to casino account and set state to prepared
        bank.deposit(player.GetStake());
        player.ResetStake();
        player.state = BlackJackPlayerState.PREPARING;
    }

    private void setWaitingBar(BlackJackPlayer player) {
        clearHotBar(player);
        PlayerInventory playerInventory = player.Player.getInventory();

        playerInventory.setItem(8, LEAVE_ITEM);
    }

    private void setBettingBar(BlackJackPlayer player) {
        clearHotBar(player);
        PlayerInventory playerInventory = player.Player.getInventory();

        playerInventory.setItem(4, HIT_ITEM);
        playerInventory.setItem(5, STAND_ITEM);
    }

    private void clearHotBar(BlackJackPlayer player) {
        PlayerInventory playerInventory = player.Player.getInventory();

        for (int i = 0; i <= 8; i++) {
            playerInventory.clear(i);
        }
    }

    private List<BlackJackPlayer> getActivePlayers() {
        return playerMap.values().stream().filter(p -> !p.state.equals(BlackJackPlayerState.PREPARING)).collect(Collectors.toList());
    }

    private BlackJackPlayer getPlayerByNumber(Integer playerNumber) {
        return playerMap.values().stream().filter(p -> p.PlayerNumber.equals(playerNumber)).findFirst().orElse(null);
    }

    private int getNextPlayerNumber() {
        List<BlackJackPlayer> players = getActivePlayers();

        for (int i = (currentPlayer + 1); i <= maxPlayers; i++) {
            int number = i;

            BlackJackPlayer player = players.stream().filter(p -> p.PlayerNumber.equals(number)).findFirst().orElse(null);
            if (player != null) return player.PlayerNumber;
        }

        return 0;
    }

    // Actions after a player wrote a message
    public void OnPlayerSendMessage(UUID playerID, String message) {
        BlackJackPlayer player = playerMap.get(playerID);
        if (player == null) return;

        if (gameState == BlackJackGameState.PREPARING && player.GetStake() == 0 && message != null && message.matches("[0-9]+")) {

            // Check for acceptable amount of money
            int amount = Integer.parseInt(message);
            if (amount >= minBet && amount <= maxBet) {

                if (economy.getBalance(player.Player) < amount) {
                    player.Player.sendMessage(ChatColor.RED + "Du hast nicht genügend Geld um diese Wette zu platzieren!");
                } else {
                    economy.withdrawPlayer(player.Player, amount);
                    player.AddMoneyToStake(amount);

                    player.Player.sendMessage(ChatColor.GREEN + "Du bist nun mit einem Einsatz von " + ChatColor.GOLD + player.GetStake() + " Tokens" +
                            ChatColor.GREEN + " in der Runde");
                }
            }
        }
    }

    @Override
    public void StartGame() {

    }

    @Override
    public void CancelGame() {

    }

    @Override
    public Location getJoinBlockPosition() {
        return joinBlockPosition;
    }

    @Override
    public Integer getMaxPlayers() {
        return minPlayers;
    }

    @Override
    public Integer getMinPlayers() {
        return maxPlayers;
    }

    @Override
    public void AddPlayer(Player player) {
        UUID playerID = player.getUniqueId();

        // Reject player to specific conditions
        if (playerMap.containsKey(playerID)) {
            player.sendMessage(ChatColor.YELLOW + "Du bist bereits mitglied dieses Spiels");
            return;
        } else if (playerMap.size() >= maxPlayers) {
            player.sendMessage(ChatColor.RED + "Das Spiel besitz bereits die maximale Anzahl an Spielern");
            return;
        } else if (economy.getBalance(player) <= minBet) {
            player.sendMessage(ChatColor.RED + "Du hast leider nicht genügend Geld, um diesem Spiel beizutreten");
            return;
        }

        // Get Free player number
        int playerNumber = 0;
        for (int i = 1; i <= maxPlayers; i++) {
            int number = i;
            if (playerMap.values().stream().noneMatch(p -> p.PlayerNumber.equals(number))) {
                playerNumber = number;
                break;
            }
        }

        // Add player to lobby
        lobby.AddPlayer(player);
        lobby.ChangePlayerState(playerID, PlayerState.INGAME);

        // Send failure message
        if (playerNumber == 0) {
            player.sendMessage(ChatColor.RED + "Beim betreten dieses Spiels ist etwas schief gelaufen");
        }

        // Create Player
        playerMap.put(playerID, new BlackJackPlayer(player, playerNumber));
        setWaitingBar(playerMap.get(playerID));
        player.sendMessage(ChatColor.GREEN + "Du bist nun mitglied dieses Spiels");

        if (gameState.equals(BlackJackGameState.WAITING) && playerMap.values().size() >= minPlayers) {
            gameState = BlackJackGameState.PREPARING;
            startPreparingTimer();
        }
    }

    @Override
    public void RemovePlayer(UUID playerID) {
        BlackJackPlayer player = playerMap.get(playerID);
        if (player != null) {
            player.Player.sendMessage(ChatColor.RED + "Du hast das BlackJack Spiel verlassen!");
            clearHotBar(player);
        }

        playerMap.remove(playerID);
        lobby.RemovePlayer(playerID);
    }

    @Override
    public boolean containsPlayer(UUID playerID) {
        return playerMap.containsKey(playerID);
    }
}
