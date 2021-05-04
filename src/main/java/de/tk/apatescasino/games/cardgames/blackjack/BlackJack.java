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

import java.util.*;
import java.util.stream.Collectors;

public class BlackJack implements Game {

    private final int minPlayers;
    private final int maxPlayers;
    private final Location joinBlockPosition;

    private final ItemStack HIT_ITEM = ItemStackBuilder.createItemStack(Material.PINK_DYE, 1, (ChatColor.DARK_GREEN + "HIT"), new String[]{"Hole dir eine weitere Karte"});
    private final ItemStack STAND_ITEM = ItemStackBuilder.createItemStack(Material.LIME_DYE, 1, (ChatColor.DARK_BLUE + "STAND"), new String[]{"Beende deinen Zug"});
    private final ItemStack LEAVE_ITEM = ItemStackBuilder.createItemStack(Material.BARRIER, 1, (ChatColor.RED + "Leave"), new String[]{"Verlasse das Spiel"});

    private final Economy economy;
    private final BankAccountHandler bank;

    private Integer currentPlayer;
    private BukkitRunnable turnTimer;


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
    }


    private void startPreparingTimer() {

    }

    private void startTurnTimer() {

    }

    private void startNewGame() {

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
        // Get current player and set waiting properties
        BlackJackPlayer player = getPlayerByNumber(currentPlayer);
        if (player != null) {
            player.state = BlackJackPlayerState.IN_GAME;
            setWaitingBar(player);
        }

        //  Get next player and set the player turn or let the croupier do his turn
        BlackJackPlayer nextPlayer = getNextPlayer();
        if (nextPlayer != null) {
            currentPlayer = nextPlayer.PlayerNumber;
            playerTurn(nextPlayer);
        } else {
            currentPlayer = 0;
            croupierTurn();
        }
    }

    private void playerTurn(BlackJackPlayer player) {

        currentPlayer = player.PlayerNumber;
        player.state = BlackJackPlayerState.BETTING;
        setBettingBar(player);

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
    }

    private void endGame() {

        for (BlackJackPlayer player : playerMap.values()) {
            if (player.getCardsValue() < croupierCardsValue) {
                playerBust(player);
            } else if (player.getCardsValue() > croupierCardsValue) {
                economy.depositPlayer(player.Player, player.getStake() * 2);
                player.ResetStake();
            } else {

            }

            player.ResetStake();
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

    private void playerHit(BlackJackPlayer player) {
        player.AddCard(cardDeck.pickFirst());

        if (player.getCardsValue() > 21) {
            playerBust(player);
        }
    }

    private void playerStand(BlackJackPlayer player) {
        nextPlayer();
    }

    private void playerBust(BlackJackPlayer player) {

        // Deposit stake of the player to casino account and set state to prepared
        bank.transferToCasino(player.Player, player.getStake());
        player.ResetStake();
        player.state = BlackJackPlayerState.PREPARING;
    }

    private void playerWon(BlackJackPlayer player, int amount) {
        bank.transferToPlayer(player.Player, amount);
        player.ResetStake();
    }

    private void setWaitingBar(BlackJackPlayer player) {
        PlayerInventory playerInventory = player.Player.getInventory();

        playerInventory.setItem(4, HIT_ITEM);
        playerInventory.setItem(5, STAND_ITEM);
    }

    private void setBettingBar(BlackJackPlayer player) {
        PlayerInventory playerInventory = player.Player.getInventory();

        playerInventory.setItem(8, LEAVE_ITEM);
    }

    private List<BlackJackPlayer> getActivePlayers() {
        return playerMap.values().stream().filter(p -> !p.state.equals(BlackJackPlayerState.IN_GAME)).collect(Collectors.toList());
    }

    private BlackJackPlayer getPlayerByNumber(Integer playerNumber) {
        return playerMap.values().stream().filter(p -> p.PlayerNumber.equals(playerNumber)).findFirst().orElse(null);
    }

    private BlackJackPlayer getNextPlayer() {
        List<BlackJackPlayer> players = getActivePlayers();

        for (int i = (currentPlayer + 1); i <= maxPlayers; i++) {
            int number = i;

            BlackJackPlayer player = players.stream().filter(p -> p.PlayerNumber.equals(number)).findFirst().orElse(null);
            if (player != null) return player;
        }

        return null;
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
        } else if (economy.getBalance(player) <= 0) {
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

        player.sendMessage(ChatColor.GREEN + "Du bist nun mitglied dieses Spiels");
    }

    @Override
    public void RemovePlayer(UUID playerID) {

        playerMap.remove(playerID);
        lobby.RemovePlayer(playerID);
    }

    @Override
    public boolean containsPlayer(UUID playerID) {
        return playerMap.containsKey(playerID);
    }
}
