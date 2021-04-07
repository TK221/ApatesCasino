package de.tk.apatescasino.games.cardgames;


import de.tk.apatescasino.ApatesCasino;
import de.tk.apatescasino.games.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

import static org.bukkit.Bukkit.getServer;


public class Poker implements Game {

    // Max and min player count which are allowed
    static final int MAX_GAME_PLAYERS = 10;
    static final int MIN_GAME_PLAYERS = 2;

    // State of the playing player
    private enum PlayerPokerState {
        PREPARING,
        WAITING,
        TURN,
        RAISING,
    }
    private enum GameState {
        WAITFORPLAYERS,
        STARTING,
        ONGOING,
    }

    private class PlayerProperties {
        public Player Player;
        public UUID playerID;
        // Individual number of the player from 1 to maximum of players
        public Integer playerNumber;
        // Amount of money the player brought to the table
        private Integer money;
        // Current Stake of the round
        private Integer stake;
        public Card FirstCard;
        public Card SecondCard;
        // The players current state of the game
        public PlayerPokerState State;

        public PlayerProperties(Player player, Integer ID, Integer money, PlayerPokerState state ) {
            this.Player = player;
            this.playerID = player.getUniqueId();
            this.playerNumber = ID;
            this.money = money;
            this.State = state;
            this.stake = 0;
        }

        public Integer getMoney() { return money; }
        public void setMoney(int amount) { money = amount; }
        public boolean removeMoney(int amount) {
            if (money >= amount) {
                money -= amount;
                return true;
            } else return false;
        }
        public void AddMoney(int amount) { money += amount; }
        public boolean transferMoneyToStake(int amount) {
            if (removeMoney(amount)) {
                addMoneyToStake(amount);
                return true;
            } else return  false;
        }
        public void transferStakeToMoney() {
            AddMoney(getMoney());
            resetStake();
        }
        private void addMoneyToStake(int amount) { stake += amount; }
        public void resetStake() { stake = 0; }
        public Integer getStake() { return stake; }
    }

    private final int minMoney;
    private final int smallBlind;
    private final int bigBlind;
    private final int minPlayers;
    private final int maxPlayers;
    private final Location joinBlockPosition;

    // Lobby which holds all the players of the game lobby
    private final Lobby lobby;
    private Integer gameCount;

    // Players to be waited for a message
    private final List<UUID> messageWaitingPlayers;
    // All players with their properties which are in the game
    private final Map<Integer, PlayerProperties> playerList;

    // Presets of betting amounts
    private final Integer[] betAmounts;
    // Items which are used to do player actions
    private final Map<Integer, ItemStack> actionItemList;
    // Items which are used to raise the stake by a specific amount
    private final Map<Integer, ItemStack> betItemList;

    // Current card deck
    private CardDeck deck;
    // Cards which are showed on the table
    private List<Card> showedCards;
    private GameState gameState;

    private Integer smallBlindPlayer;
    private Integer bigBlindPlayer;
    private Integer playerOnTurn;
    private Integer currentMinBet;
    private Integer pot;
    BukkitRunnable turnCounter;


    public Poker(Location joinBlockPosition, int smallBlind, int bigBlind, int minMoney,int minPlayers, int maxPlayers) {
        // Initialize game-settings
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.joinBlockPosition = joinBlockPosition;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.minMoney = minMoney;

        smallBlindPlayer = 0;
        bigBlindPlayer = 0;
        betAmounts = new Integer[] {5, 10, 50, 100, 500};
        gameState = GameState.WAITFORPLAYERS;

        // Initialize action items
        actionItemList = new HashMap<Integer, ItemStack>() {{
            put(0, ItemStackBuilder.createItemStack(Material.PINK_DYE, 1, (ChatColor.DARK_GREEN + "Check"), new String[] { "Skip to next player,", "if your stake is the minimum amount of the game" }));
            put(1, ItemStackBuilder.createItemStack(Material.LIME_DYE, 1, (ChatColor.DARK_BLUE + "Call"), new String[] { "Raise your stake to the minimum amount of the game" }));
            put(2, ItemStackBuilder.createItemStack(Material.LIGHT_BLUE_DYE, 1, (ChatColor.GOLD + "Raise"), new String[] { "Raise your stake" }));
            put(3, ItemStackBuilder.createItemStack(Material.LIGHT_GRAY_DYE, 1, (ChatColor.DARK_GRAY + "Fold"), new String[] { "Throw your cards, and left this round" }));
            put(8, ItemStackBuilder.createItemStack(Material.BARRIER, 1, (ChatColor.RED + "Leave"), new String[] { "Leave the game" }));
        }};
        // Initialize bet items
        betItemList = new HashMap<Integer, ItemStack>() {{
            put(0, ItemStackBuilder.createItemStack(Material.BARRIER, 1, (ChatColor.DARK_RED + "Cancel"), new String[] { "Cancel betting" }));
            put(2, ItemStackBuilder.createItemStack(Material.COAL, 1, (ChatColor.DARK_GRAY + betAmounts[0].toString() + " Tokens"), new String[] { "Raise your stake by " + betAmounts[0].toString() + " Tokens" }));
            put(3, ItemStackBuilder.createItemStack(Material.IRON_INGOT, 1, (ChatColor.AQUA + betAmounts[1].toString() + " Tokens"), new String[] { "Raise your stake by " + betAmounts[1].toString() + " Tokens" }));
            put(4, ItemStackBuilder.createItemStack(Material.GOLD_INGOT, 1, (ChatColor.DARK_GREEN + betAmounts[2].toString() + " Tokens"), new String[] { "Raise your stake by" + betAmounts[2].toString() + " Tokens" }));
            put(5, ItemStackBuilder.createItemStack(Material.DIAMOND, 1, (ChatColor.LIGHT_PURPLE + betAmounts[3].toString() + " Tokens"), new String[] { "Raise your stake by " + betAmounts[3].toString() + " Tokens" }));
            put(6, ItemStackBuilder.createItemStack(Material.EMERALD, 1, (ChatColor.DARK_PURPLE + betAmounts[4].toString() + " Tokens"), new String[] { "Raise your stake by " + betAmounts[4].toString() + " Tokens" }));
            put(8, ItemStackBuilder.createItemStack(Material.NETHER_STAR, 1, (ChatColor.GOLD + "All In"), new String[] { "Raise your stake by All your Tokens" }));
        }};

        // Initialize lobby for the players
        this.lobby = new Lobby(minPlayers, maxPlayers);

        messageWaitingPlayers = new ArrayList<>();
        playerList = new HashMap<>();
    }

    // Actions after a specific time for players
    private void waitForPlayer(String type, UUID playerID, int delayInSec) {

        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(ApatesCasino.getInstance(), new Runnable() {
            @Override
            public void run() {
                switch (type) {
                    case "setMoney":
                        // Check if the game is still waiting for a message of an player, and if its so remove him from the game
                        if (messageWaitingPlayers.contains(playerID)) {

                            if (lobby.getPlayer(playerID).getPlayerState() == PlayerState.UNREADY) {
                                RemovePlayer(playerID);

                                Player player = Bukkit.getPlayer(playerID);
                                if (player != null) player.sendMessage(ChatColor.RED + "Sorry you took to long. You left the game!");
                            }
                        }
                        break;
                }

            }
        }, delayInSec * 20L);
    }

    // Actions after a specific time for the general game
    private void waitForGame(String type, int delayInSec) {
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(ApatesCasino.getInstance(), new Runnable() {
            @Override
            public void run() {
                switch (type) {
                    case "preparing":
                        StartGame();
                        break;
                    default:
                        break;
                }
            }
        }, delayInSec * 20L);
    }

        private void startTurnTime(int playerNumber, int delayInSec) {
            turnCounter = (BukkitRunnable) new BukkitRunnable() {
                @Override
                public void run() {
                    if (playerNumber != 0) {
                        playerList.get(playerNumber).Player.sendMessage(ChatColor.RED + "Time is up. Your turn ended");
                        nextPlayer();
                    }
                    else dealerTurn();
                }
            }.runTaskLater(ApatesCasino.getInstance(), (delayInSec * 20L));
        }


    // Turn to the next player
    private void nextPlayer() {
        turnCounter.cancel();
        playerList.get(playerOnTurn).State = PlayerPokerState.WAITING;

        for (playerOnTurn = getNextActivePlayerNumber(playerOnTurn); playerOnTurn != 0; playerOnTurn = getNextActivePlayerNumber(playerOnTurn)) {
            if (playerList.get(playerOnTurn).getMoney() > 0) {
                playerTurn();
                return;
            }
        }
        for (PlayerProperties player : getActivePlayers()) {
            if (!player.getStake().equals(currentMinBet) && player.getMoney() > 0) {
                playerOnTurn = player.playerNumber;
                playerTurn();
                return;
            }
        }
        for (PlayerProperties player : getActivePlayers()) {
            if (player.getMoney().equals(0)) {
                startTurnTime(0, 5);
                return;
            }
        }
        dealerTurn();
    }

    private void playerTurn() {

    }

    private void dealerTurn() {
        for (PlayerProperties player : getActivePlayers()) {
            pot += player.getStake();
            player.resetStake();
        }

        if (showedCards.size() == 0) {
            for (int i = 0; i < 3; i++){
                showedCards.add(deck.pickFirst());
            }
        }
        else if (showedCards.size() < 5) {
            showedCards.add(deck.pickFirst());
        }
        else {
            endGame();
        }
    }

    private void endGame() {
        List<PlayerProperties> players = getActivePlayers();

        
    }

    private boolean PlayerCheck(int playerNumber) {
        PlayerProperties player = playerList.get(playerNumber);
        if (player.getStake().equals(currentMinBet)) {
            nextPlayer();
            return true;
        }
        else return false;
    }
    private boolean PlayerCall(int playerNumber) {
        PlayerProperties player = playerList.get(playerNumber);

        if (player.getStake() < currentMinBet && playerBetMoney(playerNumber, (currentMinBet - player.getStake()))) {
            nextPlayer();
            return true;
        } else return false;
    }
    private boolean PlayerRaise(int playerNumber, int amount) {
        PlayerProperties player = playerList.get(playerNumber);
        if (playerBetMoney(playerNumber ,amount)){
            if (player.State != PlayerPokerState.RAISING) player.State = PlayerPokerState.RAISING;
            return true;
        } else return false;
    }

    private boolean playerBetMoney(int playerNumber, int amount) {
        PlayerProperties player = playerList.get(playerNumber);
        if (player == null) return false;
        return player.transferMoneyToStake(amount);
    }

    private PlayerProperties getPlayerPropertiesByID(UUID playerID) {
        for (PlayerProperties playerProperties : playerList.values()) {
            if (playerProperties.playerID == playerID) return playerProperties;
        }
        return null;
    }

    private int getNextActivePlayerNumber(int currentPlayerNumber) {
        for (int number = (currentPlayerNumber + 1); number < maxPlayers; number++) {
            if (playerList.containsKey(number) && playerList.get(number).State != PlayerPokerState.PREPARING) return number;
        }
        return 0;
    }

    private List<PlayerProperties> getActivePlayers() {
        List<PlayerProperties> players = new ArrayList<>();

        for (int number = 1; number <= maxPlayers; number++) {
            if (playerList.containsKey(number) && playerList.get(number).State != PlayerPokerState.PREPARING) players.add(playerList.get(number));
        }

        players.sort(Comparator.comparing(o -> o.playerNumber));
        return players;
    }

    // Actions after a player wrote a message
    public void onPlayerSendMessage(UUID playerID, String message) {
        Player player = Bukkit.getPlayer(playerID);
        if (player == null) return;

        // Get unready player with the id and check if the message with the amount of money he will bring to the table is correct and add him to the game
        for (LobbyPlayer lobbyPlayer : lobby.getPlayersByState(PlayerState.UNREADY)) {
            if (messageWaitingPlayers.contains(playerID) && message != null && message.matches("[0-9]+")) {

                // Check for acceptable amount of money
                int amount = Integer.parseInt(message);
                if (amount >= minMoney) {

                    // Initialize player with a available number
                    for (int playerNumber = 1; playerNumber <= maxPlayers; playerNumber++) {
                        if (!playerList.containsKey(playerNumber))
                            playerList.put(playerNumber, new PlayerProperties(player, playerNumber, amount, PlayerPokerState.PREPARING));
                    }
                    messageWaitingPlayers.remove(playerID);
                    lobby.ChangePlayerState(playerID, PlayerState.READY);

                    player.sendMessage(ChatColor.GREEN +  "You are now with " + ChatColor.GOLD + amount + " Tokens" + ChatColor.GREEN + " in the game. Please wait for the next round");
                    gameState = GameState.STARTING;
                    StartGame();
                } else {
                    player.sendMessage(ChatColor.RED + "You must bring at least " + ChatColor.GOLD + minMoney + " Tokens" + ChatColor.RED + " to the table");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Please write a acceptable amount of money, the minimum amount for the table is: " + ChatColor.GOLD + minMoney + " Tokens");
            }
        }

        if (lobby.getPlayersByState(PlayerState.READY).size() > minPlayers) {
            gameState = GameState.STARTING;
            waitForGame("preparing", 10);
        }
    }

    public void PlayerAction(UUID playerID, int slot) {
        PlayerProperties player = getPlayerPropertiesByID(playerID);

        if (player.State == PlayerPokerState.TURN) {

            switch (slot) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 8:
                    break;
            }
        }
    }

    @Override
    public void StartGame() {
        if (gameState != GameState.STARTING) return;
        gameState = GameState.ONGOING;

        playerOnTurn = 0;
        pot = 0;
        currentMinBet = bigBlind;

        // Initialize card deck and shuffle it
        deck = new CardDeck();
        deck.InitStandardDeck();
        deck.ShuffleDeck();
        // Give every player two cards and set the action-items
        for (int cards = 0; cards < 2; cards++) {
            for (PlayerProperties playerProperty : playerList.values()) {
                playerProperty.State = PlayerPokerState.WAITING;

                Card card = deck.pickFirst();

                if (playerProperty.State == PlayerPokerState.PREPARING) {
                    if (cards == 0) {
                        playerProperty.FirstCard = card;
                    } else {
                        playerProperty.SecondCard = card;
                    }
                }

                Player player = playerProperty.Player;
                Inventory playerInventory = player.getInventory();

                playerInventory.setItem(cards, Card.getCardItem(card));
                for (int i = 0; i < 9; i++) if (actionItemList.containsKey(i)) playerInventory.setItem(i, actionItemList.get(i));
            }
        }
        smallBlindPlayer = getNextActivePlayerNumber(smallBlindPlayer);
        bigBlindPlayer = getNextActivePlayerNumber(smallBlindPlayer);
        playerBetMoney(smallBlindPlayer, smallBlind);
        playerBetMoney(bigBlindPlayer, bigBlind);
        playerOnTurn = bigBlindPlayer;

        // Show three cards to the table
        showedCards = new ArrayList<>();

        nextPlayer();
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
        return maxPlayers;
    }

    @Override
    public Integer getMinPlayers() {
        return minPlayers;
    }

    @Override
    public void AddPlayer(Player player) {
        UUID playerID = player.getUniqueId();

        lobby.AddPlayer(player);
        lobby.ChangePlayerState(playerID ,PlayerState.UNREADY);

        player.sendMessage(ChatColor.AQUA + "Please write how much money you bring to the table");
        messageWaitingPlayers.add(playerID);
        waitForPlayer("setMoney" ,playerID, 20);
    }

    @Override
    public void RemovePlayer(UUID playerID) {
        messageWaitingPlayers.remove(playerID);

        PlayerProperties playerProperties = getPlayerPropertiesByID(playerID);
        if (playerProperties != null) {

            playerList.remove(playerProperties.playerNumber);
        }

        lobby.RemovePlayer(playerID);
    }

    @Override
    public boolean containsPlayer(UUID playerID) {
        return lobby.getPlayer(playerID) != null;
    }
}
