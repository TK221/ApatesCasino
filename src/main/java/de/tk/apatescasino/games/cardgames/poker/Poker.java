package de.tk.apatescasino.games.cardgames.poker;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import de.tk.apatescasino.ApatesCasino;
import de.tk.apatescasino.games.Game;
import de.tk.apatescasino.games.GameType;
import de.tk.apatescasino.games.ItemStackBuilder;
import de.tk.apatescasino.games.PlayerState;
import de.tk.apatescasino.games.cardgames.card.Card;
import de.tk.apatescasino.games.lobby.Lobby;
import de.tk.apatescasino.games.utilities.PlayerInventorySaver;
import de.tk.apatescasino.games.utilities.playerBet;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getServer;


enum GameState {
    WAITFORPLAYERS,
    STARTING,
    ONGOING,
}

// State of the playing player
enum PlayerPokerState {
    PREPARING,
    WAITING,
    TURN,
    RAISING,
}

class PokerPlayerProperties {
    public org.bukkit.entity.Player Player;
    public UUID playerID;
    // Individual number of the player from 1 to maximum of players
    public Integer playerNumber;
    // Amount of money the player brought to the table
    public playerBet bet;
    public PlayerPokerHand hand;
    // The players current state of the game
    public PlayerPokerState State;

    public PokerPlayerProperties(Player player, Integer ID, Integer money, PlayerPokerState state) {
        this.Player = player;
        this.playerID = player.getUniqueId();
        this.playerNumber = ID;
        this.bet = new playerBet(money, 0);
        this.hand = new PlayerPokerHand();
        this.State = state;
    }
}

public class Poker implements Game {

    public static final String POKER_PREFIX = String.format("%sPoker%s >> ", ChatColor.DARK_PURPLE, ChatColor.WHITE);

    // Max and min player count which are allowed
    static final int MAX_GAME_PLAYERS = 10;
    static final int MIN_GAME_PLAYERS = 2;

    private final ItemStack CHECKITEM = ItemStackBuilder.createItemStack(Material.PINK_DYE, 1, (ChatColor.DARK_GREEN + "Check"), new String[]{"Übergebe deinen Zug an den nächsten Spieler", "wenn du den Mindesteinsatz beglichen hast"});
    private final ItemStack CALLITEM = ItemStackBuilder.createItemStack(Material.LIME_DYE, 1, (ChatColor.DARK_BLUE + "Call"), new String[]{"Erhöhe auf den Mindesteinsattz"});
    private final ItemStack RAISEITEM = ItemStackBuilder.createItemStack(Material.LIGHT_BLUE_DYE, 1, (ChatColor.GOLD + "Raise"), new String[]{"Erhöhe deinen Einsatz"});
    private final ItemStack FOLDITEM = ItemStackBuilder.createItemStack(Material.LIGHT_GRAY_DYE, 1, (ChatColor.DARK_GRAY + "Fold"), new String[]{"Lege deine Karten ab und verlasse die Runde"});
    private final ItemStack LEAVEITEM = ItemStackBuilder.createItemStack(Material.RED_DYE, 1, (ChatColor.RED + "Leave"), new String[]{"Verlasse das Spiel"});

    private final int minPlayers;
    private final int maxPlayers;

    private final int turnTime;
    private final int preparingTime;

    private final Location joinBlockPosition;

    // Lobby which holds all the players of the game lobby
    private final Lobby lobby;
    private final PokerCardHandler cardHandler;
    private final PokerBetHandler betHandler;

    // Players to be waited for a message
    private final List<UUID> messageWaitingPlayers;
    // All players with their properties which are in the game
    private final Map<Integer, PokerPlayerProperties> playerList;
    // Balance of all players at the beginning od a round
    private final Map<Integer, Integer> oldPlayerBalance;

    // Items which are used to raise the stake by a specific amount
    private final Map<Integer, ItemStack> betItemList;

    private GameState gameState;
    private Integer playerOnTurn;
    private BukkitTask turnCounter;
    private BukkitTask countDown;
    private Integer countDownNumber;

    private Hologram hologram;
    private TextLine gameInformationLine;
    private TextLine potLine;
    private TextLine cardLine;
    private TextLine playerInformationLine;


    public Poker(String name, Location joinBlockPosition, Location mainScreenLocation, int smallBlind, int bigBlind, int minMoney, int maxMoney, int minPlayers, int maxPlayers, int turnTime, int preparingTime, double fee) {
        messageWaitingPlayers = new ArrayList<>();
        playerList = new HashMap<>();
        oldPlayerBalance = new HashMap<>();

        // Initialize game-settings
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.turnTime = turnTime;
        this.preparingTime = preparingTime;
        this.joinBlockPosition = joinBlockPosition;

        // Initialize lobby for the players
        this.lobby = new Lobby(minPlayers, maxPlayers, name);
        this.cardHandler = new PokerCardHandler();
        this.betHandler = new PokerBetHandler(minMoney, maxMoney, smallBlind, bigBlind, fee);

        gameState = GameState.WAITFORPLAYERS;

        // Initialize bet items
        betItemList = new HashMap<Integer, ItemStack>() {{
            put(0, ItemStackBuilder.createItemStack(Material.GREEN_DYE, 1, (ChatColor.GREEN + "Fertig"), new String[]{"Erhöhen beenden"}));
            put(2, ItemStackBuilder.createItemStack(Material.COAL, 1, (ChatColor.DARK_GRAY + betHandler.betAmounts[0].toString() + " Tokens"), new String[]{"Erhöhe Einsatz um " + betHandler.betAmounts[0] + " Tokens"}));
            put(3, ItemStackBuilder.createItemStack(Material.IRON_INGOT, 1, (ChatColor.AQUA + betHandler.betAmounts[1].toString() + " Tokens"), new String[]{"Erhöhe Einsatz um " + betHandler.betAmounts[1] + " Tokens"}));
            put(4, ItemStackBuilder.createItemStack(Material.GOLD_INGOT, 1, (ChatColor.DARK_GREEN + betHandler.betAmounts[2].toString() + " Tokens"), new String[]{"Erhöhe Einsatz um" + betHandler.betAmounts[2] + " Tokens"}));
            put(5, ItemStackBuilder.createItemStack(Material.DIAMOND, 1, (ChatColor.LIGHT_PURPLE + betHandler.betAmounts[3].toString() + " Tokens"), new String[]{"Erhöhe Einsatz um " + betHandler.betAmounts[3] + " Tokens"}));
            put(6, ItemStackBuilder.createItemStack(Material.EMERALD, 1, (ChatColor.DARK_PURPLE + betHandler.betAmounts[4].toString() + " Tokens"), new String[]{"Erhöhe Einsatz um " + betHandler.betAmounts[4] + " Tokens"}));
            put(8, ItemStackBuilder.createItemStack(Material.NETHER_STAR, 1, (ChatColor.GOLD + "All In"), new String[]{"Erhöhe Einsatz um alle Tokens"}));
        }};

        Bukkit.getScheduler().scheduleSyncRepeatingTask(ApatesCasino.getInstance(), () -> {
            for (PokerPlayerProperties playerProperties : playerList.values()) {
                setPlayerMoneyToActionBar(playerProperties);
            }
        }, 0L, 20L);

        betHandler.smallBlindPlayer = 0;
        betHandler.bigBlindPlayer = 0;

        initHologram(mainScreenLocation);
    }

    // Timer for players to respond to the bet amount message
    private void writeMessageTimer(UUID playerID) {

        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(ApatesCasino.getInstance(), () -> {
            // Check if the game is still waiting for a message of an player, and if its so remove him from the game
            if (messageWaitingPlayers.contains(playerID)) {

                if (lobby.getPlayer(playerID).getPlayerState() == PlayerState.UNREADY) {
                    removePlayer(playerID);

                    Player player = Bukkit.getPlayer(playerID);
                    if (player != null)
                        player.sendMessage(POKER_PREFIX + ChatColor.RED + "Sie haben zu lange für Ihren Einsatz gebraucht und damit das Spiel verlassen");
                }
            }
        }, 30 * 20L);
    }

    // Timer to start the countdown
    private void preparingTimer() {
        if (gameState != GameState.STARTING) return;

        // Check for enough players and start the countdown else stop the game
        if (playerList.size() < minPlayers) {
            gameState = GameState.WAITFORPLAYERS;

            cardLine.setText("");
            gameInformationLine.setText(ChatColor.YELLOW + "Zurzeit kein Spiel im Gange");
            potLine.setText("");
            playerInformationLine.setText("");
        } else {
            gameCountDown();
        }
    }

    // Countdown timer
    private void gameCountDown() {
        if (gameState != GameState.STARTING) return;
        countDownNumber = preparingTime;
        countDown = new BukkitRunnable() {
            @Override
            public void run() {
                if (countDownNumber <= 0) {
                    if (playerList.size() < minPlayers) {
                        gameState = GameState.WAITFORPLAYERS;

                        cardLine.setText("");
                        gameInformationLine.setText(ChatColor.YELLOW + "Zurzeit kein Spiel im Gange");
                        potLine.setText("");
                        playerInformationLine.setText("");
                    } else {
                        startGame();
                    }
                    countDown.cancel();
                } else {
                    gameInformationLine.setText(ChatColor.YELLOW + countDownNumber.toString() + " Sekunden bis zum Start der nächsten Runde");
                    countDownNumber--;
                }
            }
        }.runTaskTimer(ApatesCasino.getInstance(), 0L, 20L);
    }

    private void startTurnTime(int playerNumber, int delayInSec) {
        turnCounter = new BukkitRunnable() {
            int timer = delayInSec;

            @Override
            public void run() {
                if (timer <= 0) {
                    if (playerNumber != 0) {
                        playerList.get(playerNumber).Player.sendMessage(POKER_PREFIX + ChatColor.RED + "Ihre Zeit ist um und der Zug beendet");
                        playerFold(playerList.get(playerNumber));
                    }
                    nextPlayer();
                } else {
                    timer--;
                    gameInformationLine.setText(ChatColor.YELLOW.toString() + timer + " Sekunden bis zum nächsten Zug");
                }

            }
        }.runTaskTimer(ApatesCasino.getInstance(), 0, 20L);
    }


    // Turn to the next player
    private void nextPlayer() {
        if (turnCounter != null) turnCounter.cancel();

        if (playerOnTurn != 0 && playerList.containsKey(playerOnTurn) && !playerList.get(playerOnTurn).State.equals(PlayerPokerState.PREPARING)) {
            setWaitingBar(playerList.get(playerOnTurn));
            playerList.get(playerOnTurn).State = PlayerPokerState.WAITING;
        }
        updateHologram();

        if (getActivePlayers().size() == 1) {
            playerOnTurn = 0;
            dealerTurn();
            return;
        }

        for (playerOnTurn = getNextActivePlayerNumber(playerOnTurn); playerOnTurn != 0; playerOnTurn = getNextActivePlayerNumber(playerOnTurn)) {
            if (playerList.get(playerOnTurn).bet.getMoney() > 0) {
                playerTurn();
                return;
            }
        }
        for (PokerPlayerProperties player : getActivePlayers()) {
            if (!player.bet.getStake().equals(betHandler.currentMinBet) && player.bet.getMoney() > 0) {
                playerOnTurn = player.playerNumber;
                playerTurn();
                return;
            }
        }
        for (PokerPlayerProperties player : getActivePlayers()) {
            if (player.bet.getMoney().equals(0)) {
                dealerTurn();
                return;
            }
        }
        dealerTurn();
    }

    private void playerTurn() {
        PokerPlayerProperties playerProperties = playerList.get(playerOnTurn);

        playerProperties.State = PlayerPokerState.TURN;
        playerProperties.Player.sendMessage(POKER_PREFIX + ChatColor.GREEN + "Sie sind nun am Zug");
        setActionItemBar(playerProperties);
        startTurnTime(playerOnTurn, turnTime);

        updateHologram();
    }

    private void dealerTurn() {
        List<PokerPlayerProperties> sidePotPlayers = getActivePlayers().stream().filter(p -> p.bet.getMoney() <= 0).collect(Collectors.toList());

        for (PokerPlayerProperties sidePlayerProperties : sidePotPlayers) {
            int currPot = betHandler.pot;
            int stake = sidePlayerProperties.bet.getStake();

            for (PokerPlayerProperties playerProperties : getActivePlayers())
                if (stake < playerProperties.bet.getStake())
                    currPot = currPot - (playerProperties.bet.getStake() - stake);

            //System.out.println("Sidepot: " + sidePlayerProperties.Player.getDisplayName() + " - " + currPot);
            betHandler.sidePots.put(sidePlayerProperties.playerNumber, currPot);
        }

        if (cardHandler.showedCards.size() == 0) {
            for (int i = 0; i < 3; i++) {
                cardHandler.showedCards.add(cardHandler.deck.pickFirst());
            }
            startTurnTime(0, 0);
        } else if (cardHandler.showedCards.size() < 5) {
            cardHandler.showedCards.add(cardHandler.deck.pickFirst());
            startTurnTime(0, 0);
        } else {
            endGame();
        }

        updateHologram();
    }

    private void endGame() {
        turnCounter.cancel();

        if (getActivePlayers().size() != 0) {
            for (PokerPlayerProperties playerProperties : playerList.values()) {
                if (!oldPlayerBalance.containsKey(playerProperties.playerNumber))
                    oldPlayerBalance.put(playerProperties.playerNumber, playerProperties.bet.getMoney());
            }


            List<List<PokerPlayerProperties>> playerWinOrder = cardHandler.getWinners(getActivePlayers());

            /*
            System.out.println("-----");
            int i = 0;
            for (List<PokerPlayerProperties> playerPropertiesList : playerWinOrder) {
                System.out.println(i);
                for (PokerPlayerProperties playerProperties : playerPropertiesList) {
                    System.out.println("Spieler " + playerProperties.Player.getDisplayName());
                }
                i++;
            }
            System.out.println("-----"); */

            betHandler.distributeMoney(playerWinOrder);
            for (PokerPlayerProperties playerProperties : playerList.values()) playerProperties.bet.resetStake();

            List<PokerPlayerProperties> winners = playerWinOrder.get(0);

            String endMessage = getEndMessage(winners, oldPlayerBalance);
            playerList.values().forEach(p -> p.Player.sendMessage(POKER_PREFIX + endMessage));
        }

        Set<Integer> playerNumbers = new HashSet<>(playerList.keySet());
        for (Integer playerNumber : playerNumbers) {
            PokerPlayerProperties playerProperties = playerList.get(playerNumber);
            setPreparingBar(playerProperties);

            if (playerProperties.bet.getMoney() <= 0) removePlayer(playerProperties.playerID);
            else playerProperties.State = PlayerPokerState.PREPARING;
        }

        gameState = GameState.STARTING;
        preparingTimer();
    }

    private String getEndMessage(List<PokerPlayerProperties> winners, Map<Integer, Integer> oldPlayerBalance) {
        List<PokerPlayerProperties> activePlayers = getActivePlayers();

        List<PokerPlayerProperties> otherPlayers = new ArrayList<>(playerList.values());
        otherPlayers.removeIf(activePlayers::contains);

        activePlayers.removeIf(winners::contains);
        activePlayers.sort(Comparator.comparing(p -> p.hand.hand.ordinal()));
        Collections.reverse(activePlayers);

        StringBuilder message = new StringBuilder(ChatColor.GREEN + "--- Gewonnen ---\n");
        winners.forEach(w -> message.append(ChatColor.YELLOW).append(w.Player.getDisplayName()).append(": ").append(PokerCardHandler.getCardHandText(w.hand))
                .append(" - ").append(PokerBetHandler.getBalanceDifferenceText(w.bet.getMoney(), oldPlayerBalance.get(w.playerNumber))));

        message.append(ChatColor.RED).append("\n--- Verloren ---\n");
        activePlayers.forEach(p -> message.append(ChatColor.YELLOW).append(p.Player.getDisplayName()).append(": ").append(PokerCardHandler.getCardHandText(p.hand))
                .append(" - ").append(PokerBetHandler.getBalanceDifferenceText(p.bet.getMoney(), oldPlayerBalance.get(p.playerNumber))));

        message.append(ChatColor.DARK_AQUA).append("\n--- Ausgeschieden ---\n");
        otherPlayers.forEach(o -> message.append(ChatColor.YELLOW).append(o.Player.getDisplayName()).append(": ")
                .append(PokerBetHandler.getBalanceDifferenceText(o.bet.getMoney(), oldPlayerBalance.get(o.playerNumber))));
        message.append(ChatColor.WHITE).append("\n--- Ende ---");

        return message.toString();
    }

    private void playerCheckCall(PokerPlayerProperties playerProperties) {
        playerBet playerBet = playerProperties.bet;

        if (playerBet.getStake() >= betHandler.currentMinBet) {
            nextPlayer();
        } else if (playerBet.getMoney() >= (betHandler.currentMinBet - playerBet.getStake())) {
            betHandler.playerBetMoney(playerBet, (betHandler.currentMinBet - playerBet.getStake()));
            nextPlayer();
        } else {
            betHandler.playerBetMoney(playerBet, playerBet.getMoney());
            nextPlayer();
        }
    }

    private void playerBetting(PokerPlayerProperties playerProperties) {
        playerBet playerBet = playerProperties.bet;

        if (playerBet.getStake() < betHandler.currentMinBet) {
            if (playerBet.getMoney() > (betHandler.currentMinBet - playerBet.getStake())) {
                betHandler.playerBetMoney(playerBet, (betHandler.currentMinBet - playerBet.getStake()));
            } else {
                betHandler.playerBetMoney(playerBet, playerBet.getMoney());
                nextPlayer();
                return;
            }
        }

        playerProperties.State = PlayerPokerState.RAISING;
        new BukkitRunnable() {
            @Override
            public void run() {
                setBetItemBar(playerProperties);
            }
        }.runTaskLater(ApatesCasino.getInstance(), 20L);

    }

    private boolean playerRaise(PokerPlayerProperties playerProperties, int amount) {
        if (betHandler.playerBetMoney(playerProperties.bet, amount)) {
            if (playerProperties.State != PlayerPokerState.RAISING) playerProperties.State = PlayerPokerState.RAISING;
            return true;
        } else return false;
    }

    private void playerFold(PokerPlayerProperties playerProperties) {
        playerProperties.State = PlayerPokerState.PREPARING;
        playerProperties.bet.resetStake();
        setPreparingBar(playerProperties);

        if (playerOnTurn.equals(playerProperties.playerNumber)) nextPlayer();

        Inventory playerInventory = playerProperties.Player.getInventory();

        playerInventory.clear(0);
        playerInventory.clear(1);
    }

    private void setActionItemBar(PokerPlayerProperties playerProperties) {
        PlayerInventory playerInventory = playerList.get(playerProperties.playerNumber).Player.getInventory();
        clearHotBar(playerProperties);

        if (playerProperties.bet.getStake() >= betHandler.currentMinBet) playerInventory.setItem(3, CHECKITEM);
        else playerInventory.setItem(3, CALLITEM);
        playerInventory.setItem(4, RAISEITEM);
        playerInventory.setItem(5, FOLDITEM);

        playerInventory.setItem(0, Card.getCardItem(playerProperties.hand.firstCard));
        playerInventory.setItem(1, Card.getCardItem(playerProperties.hand.secondCard));
    }

    private void setBetItemBar(PokerPlayerProperties playerProperties) {
        if (playerProperties == null) return;
        PlayerInventory inventory = playerProperties.Player.getInventory();

        clearHotBar(playerProperties);

        for (int i = 0; i < 9; i++) if (betItemList.containsKey(i)) inventory.setItem(i, betItemList.get(i));
    }

    private void setPreparingBar(PokerPlayerProperties playerProperties) {
        PlayerInventory playerInventory = playerList.get(playerProperties.playerNumber).Player.getInventory();
        clearHotBar(playerProperties);

        playerInventory.setItem(7, LEAVEITEM);
    }

    public void setWaitingBar(PokerPlayerProperties playerProperties) {
        PlayerInventory playerInventory = playerList.get(playerProperties.playerNumber).Player.getInventory();
        clearHotBar(playerProperties);

        if (playerProperties.hand.firstCard != null && playerProperties.hand.secondCard != null) {
            playerInventory.setItem(0, Card.getCardItem(playerProperties.hand.firstCard));
            playerInventory.setItem(1, Card.getCardItem(playerProperties.hand.secondCard));
        }
    }

    private void clearHotBar(PokerPlayerProperties playerProperties) {
        if (playerProperties == null) return;
        PlayerInventory inventory = playerProperties.Player.getInventory();

        for (int i = 0; i < 9; i++) inventory.clear(i);
    }

    private void setPlayerMoneyToActionBar(PokerPlayerProperties playerProperties) {
        if (playerProperties == null) return;
        playerBet playerBet = playerProperties.bet;

        String message = "Geld: " + ChatColor.GOLD + playerBet.getMoney() + ChatColor.WHITE + "  Einsatz: " + ChatColor.GOLD + playerBet.getStake();
        playerProperties.Player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    private PokerPlayerProperties getPlayerPropertiesByID(UUID playerID) {
        for (PokerPlayerProperties pokerPlayerProperties : playerList.values()) {
            if (pokerPlayerProperties.playerID == playerID) return pokerPlayerProperties;
        }
        return null;
    }

    private int getNextActivePlayerNumber(int currentPlayerNumber) {
        for (int number = (currentPlayerNumber + 1); number < maxPlayers; number++) {
            if (playerList.containsKey(number) && playerList.get(number).State != PlayerPokerState.PREPARING) {
                return number;
            }
        }
        return 0;
    }

    private List<PokerPlayerProperties> getActivePlayers() {
        List<PokerPlayerProperties> players = new ArrayList<>();

        for (int number = 1; number <= maxPlayers; number++) {
            if (playerList.containsKey(number) && playerList.get(number).State != PlayerPokerState.PREPARING)
                players.add(playerList.get(number));
        }

        players.sort(Comparator.comparing(o -> o.playerNumber));
        return players;
    }

    private void initHologram(Location screenLocation) {
        hologram = HologramsAPI.createHologram(ApatesCasino.getInstance(), screenLocation.add(0.5, 3.5, 0.5));

        hologram.insertTextLine(0, ChatColor.BLUE + "Poker: " + lobby.id);
        hologram.insertTextLine(1, ChatColor.WHITE + "-------");
        hologram.insertTextLine(2, "");
        gameInformationLine = hologram.insertTextLine(3, ChatColor.YELLOW + "Zurzeit kein Spiel im Gange");
        potLine = hologram.insertTextLine(4, "");
        cardLine = hologram.insertTextLine(5, "");
        playerInformationLine = hologram.insertTextLine(6, "");
    }

    private void updateHologram() {

        StringBuilder cards = new StringBuilder("| ");
        for (int i = 0; i < cardHandler.showedCards.size(); i++) {
            Card card = cardHandler.showedCards.get(i);
            cards.append(Card.getTextCard(card)).append(" | ");
        }
        gameInformationLine.setText(ChatColor.AQUA + "Im Spiel");
        cardLine.setText("Offene Karten: " + cards);
        potLine.setText("Pot: " + ChatColor.GOLD + betHandler.pot + " | Mindesteinsatz: " + betHandler.currentMinBet);
        playerInformationLine.setText("Am Zug: " + ((playerOnTurn.equals(0) || !playerList.containsKey(playerOnTurn)) ? (ChatColor.AQUA + "Dealer") : playerList.get(playerOnTurn).Player.getDisplayName()));
    }

    // Actions after a player wrote a message
    public void onPlayerSendMessage(UUID playerID, String message) {
        Player player = Bukkit.getPlayer(playerID);
        if (player == null) return;

        // Get unready player with the id and check if the message with the amount of money he will bring to the table is correct and add him to the game

        final String betMessage = POKER_PREFIX + ChatColor.RED + "Geben sie bitte einen passenden Betrag ein (MIN: " + ChatColor.GOLD + betHandler.minMoney + ChatColor.AQUA +
                " | MAX: " + ChatColor.GOLD + betHandler.maxMoney + ChatColor.AQUA + ")";
        if (messageWaitingPlayers.contains(playerID) && message != null && message.matches("[0-9]+")) {

            // Check for acceptable amount of money
            int amount = Integer.parseInt(message);
            if (amount >= betHandler.minMoney) {

                Economy econ = ApatesCasino.getEconomy();

                if (econ.getBalance(player) < betHandler.minMoney) {
                    player.sendMessage(POKER_PREFIX + ChatColor.RED + "Sie haben nicht genügend Geld um diesem Spiel beizutreten. Mindestens: " + betHandler.minMoney + " Tokens");
                    messageWaitingPlayers.remove(playerID);
                    return;
                }
                econ.withdrawPlayer(player, amount);

                // Initialize player with a available number
                for (int playerNumber = 1; playerNumber <= maxPlayers; playerNumber++) {
                    if (!playerList.containsKey(playerNumber)) {

                        playerList.put(playerNumber, new PokerPlayerProperties(player, playerNumber, amount, PlayerPokerState.PREPARING));
                        break;
                    }
                }
                messageWaitingPlayers.remove(playerID);
                lobby.changePlayerState(playerID, PlayerState.READY);

                player.sendMessage(POKER_PREFIX + ChatColor.GREEN + "Sie sind nun mit " + ChatColor.GOLD + amount + " Tokens" + ChatColor.GREEN + " im Spiel. Bitte warten sie auf den Beginn der nächsten Runde");

                PlayerInventorySaver.addPlayerInventory(player);

                PokerPlayerProperties playerProperties = getPlayerPropertiesByID(playerID);
                if (playerProperties != null) setPreparingBar(playerProperties);
            } else {
                player.sendMessage(betMessage);
            }
        } else {
            player.sendMessage(betMessage);
        }

        if (playerList.size() >= minPlayers && gameState == GameState.WAITFORPLAYERS) {
            gameState = GameState.STARTING;
            preparingTimer();
        }
    }

    public void playerAction(UUID playerID, int slot) {
        PokerPlayerProperties playerProperties = getPlayerPropertiesByID(playerID);
        if (playerProperties == null) return;
        Player player = playerProperties.Player;

        if (playerProperties.State.equals(PlayerPokerState.TURN) && gameState == GameState.ONGOING) {
            switch (slot) {
                case 3:
                    playerCheckCall(playerProperties);
                    break;
                case 4:
                    playerBetting(playerProperties);
                    break;
                case 5:
                    playerFold(playerProperties);
                    break;
                case 7:
                    removePlayer(playerProperties.playerID);
                    break;
            }
        } else if (playerProperties.State.equals(PlayerPokerState.RAISING) && gameState == GameState.ONGOING) {
            switch (slot) {
                case 0:
                    setPreparingBar(playerProperties);
                    nextPlayer();
                    break;
                case 2:
                    if (!playerRaise(playerProperties, betHandler.betAmounts[0]))
                        player.sendMessage(POKER_PREFIX + ChatColor.RED + "Sie haben nicht genügent Geld");
                    break;
                case 3:
                    if (!playerRaise(playerProperties, betHandler.betAmounts[1]))
                        player.sendMessage(POKER_PREFIX + ChatColor.RED + "Sie haben nicht genügent Geld");
                    break;
                case 4:
                    if (!playerRaise(playerProperties, betHandler.betAmounts[2]))
                        player.sendMessage(POKER_PREFIX + ChatColor.RED + "Sie haben nicht genügent Geld");
                    break;
                case 5:
                    if (!playerRaise(playerProperties, betHandler.betAmounts[3]))
                        player.sendMessage(POKER_PREFIX + ChatColor.RED + "Sie haben nicht genügent Geld");
                    break;
                case 6:
                    if (!playerRaise(playerProperties, betHandler.betAmounts[4]))
                        player.sendMessage(POKER_PREFIX + ChatColor.RED + "Sie haben nicht genügent Geld");
                    break;
                case 8:
                    if (!playerRaise(playerProperties, playerProperties.bet.getMoney()))
                        player.sendMessage(POKER_PREFIX + ChatColor.RED + "Sie haben nicht genügent Geld");
                    else nextPlayer();
                    break;
            }

            setPlayerMoneyToActionBar(playerProperties);
        } else {
            if (slot == 7) {
                removePlayer(playerID);
            }
            return;
        }

        updateHologram();
    }

    @Override
    public void startGame() {
        if (gameState != GameState.STARTING) return;
        gameState = GameState.ONGOING;

        if (turnCounter != null) turnCounter.cancel();
        if (countDown != null) countDown.cancel();

        /*
        System.out.println("--- New Game ---");
        playerList.values().forEach(p -> System.out.println(p.Player.getDisplayName()));
        System.out.println("----"); */

        playerList.values().forEach(p -> p.Player.sendMessage(POKER_PREFIX + ChatColor.GREEN + "Die Runde startet nun"));

        playerOnTurn = 0;
        betHandler.pot = 0;
        betHandler.sidePots = new HashMap<>();
        betHandler.currentMinBet = betHandler.bigBlind;

        cardHandler.initGameCardHandler();

        // Give every player two cards and set the action-items
        for (PokerPlayerProperties playerProperty : playerList.values()) {
            playerProperty.State = PlayerPokerState.WAITING;

            cardHandler.initPlayerPokerCards(playerProperty.hand);

            Player player = playerProperty.Player;
            Inventory playerInventory = player.getInventory();

            playerInventory.setItem(0, Card.getCardItem(playerProperty.hand.firstCard));
            playerInventory.setItem(1, Card.getCardItem(playerProperty.hand.secondCard));
            setWaitingBar(playerProperty);
            setPlayerMoneyToActionBar(playerProperty);
        }

        int feeAmount = (int) Math.ceil(betHandler.bigBlind * betHandler.fee);

        for (PokerPlayerProperties playerProperties : playerList.values()) {

            if (betHandler.fee > 0.0 && playerProperties.bet.getMoney() > betHandler.bigBlind + feeAmount) {
                playerProperties.Player.sendMessage(POKER_PREFIX + "Sie zahlen eine Gebühr von " + feeAmount + " an das Casino");
                playerProperties.bet.removeMoney(feeAmount);
                ApatesCasino.getBankAccountHandler().transferToCasino(playerProperties.Player.getPlayer(), feeAmount);
            }

            oldPlayerBalance.put(playerProperties.playerNumber, playerProperties.bet.getMoney());
        }

        if (getActivePlayers().size() > 1) {
            betHandler.smallBlindPlayer = getNextActivePlayerNumber(betHandler.smallBlindPlayer);
            if (betHandler.smallBlindPlayer == 0) betHandler.smallBlindPlayer = getNextActivePlayerNumber(0);
            betHandler.bigBlindPlayer = getNextActivePlayerNumber(betHandler.smallBlindPlayer);
            if (betHandler.bigBlindPlayer == 0) betHandler.bigBlindPlayer = getNextActivePlayerNumber(0);

            betHandler.playerBetMoney(playerList.get(betHandler.smallBlindPlayer).bet, betHandler.smallBlind);
            betHandler.playerBetMoney(playerList.get(betHandler.bigBlindPlayer).bet, betHandler.bigBlind);
            playerOnTurn = betHandler.bigBlindPlayer;
            playerOnTurn = 1;
        }

        nextPlayer();
    }

    @Override
    public void cancelGame() {
        playerList.values().forEach(p -> removePlayer(p.playerID));

        hologram.delete();
        playerList.clear();
    }

    @Override
    public GameType getGameType() {
        return GameType.POKER;
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
    public void addPlayer(Player player) {
        Economy econ = ApatesCasino.getEconomy();

        if (econ.getBalance(player) < betHandler.minMoney) {
            player.sendMessage(POKER_PREFIX + ChatColor.RED + "Sie haben nicht genügend Geld üm diesem Spiel beizutreten! (MIN: " + ChatColor.GOLD + betHandler.minMoney + ChatColor.RED + ")");
            return;
        }

        UUID playerID = player.getUniqueId();

        lobby.addPlayer(player);
        lobby.changePlayerState(playerID, PlayerState.UNREADY);

        player.sendMessage(POKER_PREFIX + ChatColor.AQUA + "Bitte schreiben sie ihren Geldbetrag den sie zum Tisch bringen wollen (MIN: " + ChatColor.GOLD + betHandler.minMoney + ChatColor.AQUA +
                " | MAX: " + ChatColor.GOLD + betHandler.maxMoney + ChatColor.AQUA + ")");
        messageWaitingPlayers.add(playerID);
        writeMessageTimer(playerID);
    }

    @Override
    public void removePlayer(UUID playerID) {
        messageWaitingPlayers.remove(playerID);

        PokerPlayerProperties playerProperties = getPlayerPropertiesByID(playerID);
        if (playerProperties != null) {
            if (playerProperties.Player != null) {
                ApatesCasino.getEconomy().depositPlayer(playerProperties.Player, playerProperties.bet.getMoney());
                clearHotBar(playerProperties);

                playerProperties.Player.sendMessage(POKER_PREFIX + ChatColor.RED + "Sie haben das Spiel verlassen");
                PlayerInventorySaver.setPlayerInventory(playerProperties.Player);
            }

            playerList.remove(playerProperties.playerNumber);

            PlayerPokerState playerState = playerProperties.State;
            if (playerState.equals(PlayerPokerState.TURN) || playerState.equals(PlayerPokerState.RAISING)) nextPlayer();
        }

        lobby.removePlayer(playerID);
    }

    @Override
    public boolean containsPlayer(UUID playerID) {
        return lobby.getPlayer(playerID) != null;
    }

    public static Game createGame(PokerConfig config) {
        return new Poker(config.gameID, config.joinBlockPosition.getLocation(), config.MainScreenLocation.getLocation(), config.smallBlind, config.bigBlind,
                config.minMoney, config.maxMoney, config.minPlayers, config.maxPlayers, config.turnTime, config.preparingTime, config.fee);
    }
}