package de.tk.apatescasino.games.cardgames.poker;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import de.tk.apatescasino.ApatesCasino;
import de.tk.apatescasino.games.Game;
import de.tk.apatescasino.games.ItemStackBuilder;
import de.tk.apatescasino.games.Lobby;
import de.tk.apatescasino.games.PlayerState;
import de.tk.apatescasino.games.cardgames.card.Card;
import de.tk.apatescasino.games.utilities.PlayerBet;
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
    public PlayerBet bet;
    public PlayerPokerHand hand;
    // The players current state of the game
    public PlayerPokerState State;

    public PokerPlayerProperties(Player player, Integer ID, Integer money, PlayerPokerState state) {
        this.Player = player;
        this.playerID = player.getUniqueId();
        this.playerNumber = ID;
        this.bet = new PlayerBet(money, 0);
        this.hand = new PlayerPokerHand();
        this.State = state;
    }
}

public class Poker implements Game {

    // Max and min player count which are allowed
    static final int MAX_GAME_PLAYERS = 10;
    static final int MIN_GAME_PLAYERS = 2;

    private final ItemStack CHECKITEM = ItemStackBuilder.createItemStack(Material.PINK_DYE, 1, (ChatColor.DARK_GREEN + "Check"), new String[]{"Übergebe deinen Zug an den nächsten Spieler", "wenn du den Mindesteinsatz beglichen hast"});
    private final ItemStack CALLITEM = ItemStackBuilder.createItemStack(Material.LIME_DYE, 1, (ChatColor.DARK_BLUE + "Call"), new String[]{"Erhöhe auf den Mindesteinsattz"});
    private final ItemStack RAISEITEM = ItemStackBuilder.createItemStack(Material.LIGHT_BLUE_DYE, 1, (ChatColor.GOLD + "Raise"), new String[]{"Erhöhe deinen Einsatz"});
    private final ItemStack FOLDITEM = ItemStackBuilder.createItemStack(Material.LIGHT_GRAY_DYE, 1, (ChatColor.DARK_GRAY + "Fold"), new String[]{"Lege deine Karten ab und verlasse die Runde"});
    private final ItemStack LEAVEITEM = ItemStackBuilder.createItemStack(Material.BARRIER, 1, (ChatColor.RED + "Leave"), new String[]{"Verlasse das Spiel"});

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
    private final Map<Integer, Integer> oldPlayerBalance;

    // Items which are used to raise the stake by a specific amount
    private final Map<Integer, ItemStack> betItemList;

    private GameState gameState;
    private Integer playerOnTurn;
    private BukkitTask turnCounter;
    private BukkitTask countDown;
    private Integer countDownNumber;

    private Hologram hologram;
    private TextLine potLine;
    private TextLine cardLine;
    private TextLine playerInformationLine;


    public Poker(String name, Location joinBlockPosition, int smallBlind, int bigBlind, int minMoney, int minPlayers, int maxPlayers, int turnTime, int preparingTime) {
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
        this.betHandler = new PokerBetHandler(minMoney, smallBlind, bigBlind);

        gameState = GameState.WAITFORPLAYERS;

        // Initialize bet items
        betItemList = new HashMap<Integer, ItemStack>() {{
            put(0, ItemStackBuilder.createItemStack(Material.GREEN_DYE, 1, (ChatColor.GREEN + "Fertig"), new String[]{"Erhöhen beenden"}));
            put(2, ItemStackBuilder.createItemStack(Material.COAL, 1, (ChatColor.DARK_GRAY + betHandler.BetAmounts[0].toString() + " Tokens"), new String[]{"Erhöhe deinen Einsatz um " + betHandler.BetAmounts[0] + " Tokens"}));
            put(3, ItemStackBuilder.createItemStack(Material.IRON_INGOT, 1, (ChatColor.AQUA + betHandler.BetAmounts[1].toString() + " Tokens"), new String[]{"Erhöhe deinen Einsatz um " + betHandler.BetAmounts[1] + " Tokens"}));
            put(4, ItemStackBuilder.createItemStack(Material.GOLD_INGOT, 1, (ChatColor.DARK_GREEN + betHandler.BetAmounts[2].toString() + " Tokens"), new String[]{"Erhöhe deinen Einsatz um" + betHandler.BetAmounts[2] + " Tokens"}));
            put(5, ItemStackBuilder.createItemStack(Material.DIAMOND, 1, (ChatColor.LIGHT_PURPLE + betHandler.BetAmounts[3].toString() + " Tokens"), new String[]{"Erhöhe deinen Einsatz um " + betHandler.BetAmounts[3] + " Tokens"}));
            put(6, ItemStackBuilder.createItemStack(Material.EMERALD, 1, (ChatColor.DARK_PURPLE + betHandler.BetAmounts[4].toString() + " Tokens"), new String[]{"Erhöhe deinen Einsatz um " + betHandler.BetAmounts[4] + " Tokens"}));
            put(8, ItemStackBuilder.createItemStack(Material.NETHER_STAR, 1, (ChatColor.GOLD + "All In"), new String[]{"Erhöhe deinen Einsatz um all deine Tokens"}));
        }};

        Bukkit.getScheduler().scheduleSyncRepeatingTask(ApatesCasino.getInstance(), () -> {
            for (PokerPlayerProperties playerProperties : playerList.values()) {
                setPlayerMoneyToActionBar(playerProperties);
            }
        }, 0L, 20L);

        betHandler.SmallBlindPlayer = 0;
        betHandler.BigBlindPlayer = 0;

        initHologram();
    }

    // Actions after a specific time for players
    private void writeMessageTimer(UUID playerID) {

        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(ApatesCasino.getInstance(), () -> {
            // Check if the game is still waiting for a message of an player, and if its so remove him from the game
            if (messageWaitingPlayers.contains(playerID)) {

                if (lobby.getPlayer(playerID).getPlayerState() == PlayerState.UNREADY) {
                    RemovePlayer(playerID);

                    Player player = Bukkit.getPlayer(playerID);
                    if (player != null)
                        player.sendMessage(ChatColor.RED + "Sorry du hast zu lange gebraucht, du hast das Spiel verlassen");
                }
            }
        }, 30 * 20L);
    }

    // Actions after a specific time for the general game
    private void preparingTimer() {
        if (gameState != GameState.STARTING) return;
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(ApatesCasino.getInstance(), () -> {

            if (playerList.size() < minPlayers) {
                gameState = GameState.WAITFORPLAYERS;

                cardLine.setText("Karten:");
                potLine.setText("Pot:");
                playerInformationLine.setText("Am Zug:");
            } else {
                gameCountDown();
            }
        }, preparingTime * 20L);
    }

    private void gameCountDown() {
        if (gameState != GameState.STARTING) return;
        countDownNumber = 15;
        countDown = new BukkitRunnable() {
            @Override
            public void run() {
                if (countDownNumber <= 0) {
                    if (playerList.size() < minPlayers) {
                        gameState = GameState.WAITFORPLAYERS;

                        cardLine.setText("Karten:");
                        potLine.setText("Pot:");
                        playerInformationLine.setText("Am Zug:");
                    } else {
                        System.out.println(countDownNumber);
                        StartGame();
                    }
                    countDown.cancel();
                } else if (countDownNumber % 5 == 0)
                    playerList.values().forEach(p -> p.Player.sendMessage(ChatColor.YELLOW + countDownNumber.toString() + " Sekunden bis zum Start der nächsten Runde"));

                countDownNumber--;
                System.out.println(countDownNumber);
            }
        }.runTaskTimer(ApatesCasino.getInstance(), 0L, 20L);
    }

    private void startTurnTime(int playerNumber, int delayInSec) {
        turnCounter = new BukkitRunnable() {
            @Override
            public void run() {
                if (playerNumber != 0) {
                    playerList.get(playerNumber).Player.sendMessage(ChatColor.RED + "Deine Zeit nun um, dein Zug ist beendet");
                    playerFold(playerList.get(playerNumber));
                }
                nextPlayer();
            }
        }.runTaskLater(ApatesCasino.getInstance(), (delayInSec * 20L));
    }


    // Turn to the next player
    private void nextPlayer() {
        if (turnCounter != null) turnCounter.cancel();

        if (playerOnTurn != 0 && playerList.containsKey(playerOnTurn) && !playerList.get(playerOnTurn).State.equals(PlayerPokerState.PREPARING)) {
            setWaitingItemBar(playerList.get(playerOnTurn));
            playerList.get(playerOnTurn).State = PlayerPokerState.WAITING;
        }
        updateHologram();

        if (getActivePlayers().size() == 1) {
            playerOnTurn = 0;
            dealerTurn();
            return;
        }

        for (playerOnTurn = getNextActivePlayerNumber(playerOnTurn); playerOnTurn != 0; playerOnTurn = getNextActivePlayerNumber(playerOnTurn)) {
            if (playerList.get(playerOnTurn).bet.GetMoney() > 0) {
                playerTurn();
                return;
            }
        }
        for (PokerPlayerProperties player : getActivePlayers()) {
            if (!player.bet.GetStake().equals(betHandler.CurrentMinBet) && player.bet.GetMoney() > 0) {
                playerOnTurn = player.playerNumber;
                playerTurn();
                return;
            }
        }
        for (PokerPlayerProperties player : getActivePlayers()) {
            if (player.bet.GetMoney().equals(0)) {
                dealerTurn();
                return;
            }
        }
        dealerTurn();
    }

    private void playerTurn() {
        PokerPlayerProperties playerProperties = playerList.get(playerOnTurn);

        playerProperties.State = PlayerPokerState.TURN;
        playerProperties.Player.sendMessage(ChatColor.GREEN + "Du bist nun am Zug");
        setActionItemBar(playerProperties);
        startTurnTime(playerOnTurn, 30);

        updateHologram();
    }

    private void dealerTurn() {
        List<PokerPlayerProperties> sidePotPlayers = getActivePlayers().stream().filter(p -> p.bet.GetMoney() <= 0).collect(Collectors.toList());

        for (PokerPlayerProperties sidePlayerProperties : sidePotPlayers) {
            int currPot = betHandler.Pot;
            int stake = sidePlayerProperties.bet.GetStake();

            for (PokerPlayerProperties playerProperties : getActivePlayers())
                if (stake < playerProperties.bet.GetStake())
                    currPot = currPot - (playerProperties.bet.GetStake() - stake);

            System.out.println("Sidepot: " + sidePlayerProperties.Player.getDisplayName() + " - " + currPot);
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
                    oldPlayerBalance.put(playerProperties.playerNumber, playerProperties.bet.GetMoney());
            }


            List<List<PokerPlayerProperties>> playerWinOrder = cardHandler.getWinners(getActivePlayers());

            System.out.println("-----");
            int i = 0;
            for (List<PokerPlayerProperties> playerPropertiesList : playerWinOrder) {
                System.out.println(i);
                for (PokerPlayerProperties playerProperties : playerPropertiesList) {
                    System.out.println("Player " + playerProperties.Player.getDisplayName());
                }
                i++;
            }
            System.out.println("-----");

            betHandler.DistributeMoney(playerWinOrder);
            for (PokerPlayerProperties playerProperties : playerList.values()) playerProperties.bet.ResetStake();

            List<PokerPlayerProperties> winners = playerWinOrder.get(0);

            String endMessage = getEndMessage(winners, oldPlayerBalance);
            playerList.values().forEach(p -> p.Player.sendMessage(endMessage));
        }

        Set<Integer> playerNumbers = new HashSet<>(playerList.keySet());
        for (Integer playerNumber : playerNumbers) {
            PokerPlayerProperties playerProperties = playerList.get(playerNumber);

            if (playerProperties.bet.GetMoney() <= 0) RemovePlayer(playerProperties.playerID);
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
        activePlayers.sort(Comparator.comparing(p -> p.hand.Hand.ordinal()));
        Collections.reverse(activePlayers);

        StringBuilder message = new StringBuilder(ChatColor.GREEN + "--- Gewonnen ---\n");
        winners.forEach(w -> message.append(ChatColor.YELLOW).append(w.Player.getDisplayName()).append(": ").append(PokerCardHandler.GetCardHandText(w.hand))
                .append(" - ").append(PokerBetHandler.getBalanceDifferenceText(w.bet.GetMoney(), oldPlayerBalance.get(w.playerNumber))));

        message.append(ChatColor.RED).append("\n--- Verloren ---\n");
        activePlayers.forEach(p -> message.append(ChatColor.YELLOW).append(p.Player.getDisplayName()).append(": ").append(PokerCardHandler.GetCardHandText(p.hand))
                .append(" - ").append(PokerBetHandler.getBalanceDifferenceText(p.bet.GetMoney(), oldPlayerBalance.get(p.playerNumber))));

        message.append(ChatColor.DARK_AQUA).append("\n--- Ausgeschieden ---\n");
        otherPlayers.forEach(o -> message.append(ChatColor.YELLOW).append(o.Player.getDisplayName()).append(": ")
                .append(PokerBetHandler.getBalanceDifferenceText(o.bet.GetMoney(), oldPlayerBalance.get(o.playerNumber))));
        message.append(ChatColor.WHITE).append("\n--- Ende ---");

        return message.toString();
    }

    private void playerCheckCall(PokerPlayerProperties playerProperties) {
        PlayerBet playerBet = playerProperties.bet;

        if (playerBet.GetStake() >= betHandler.CurrentMinBet) {
            nextPlayer();
        } else if (playerBet.GetMoney() >= (betHandler.CurrentMinBet - playerBet.GetStake())) {
            betHandler.PlayerBetMoney(playerBet, (betHandler.CurrentMinBet - playerBet.GetStake()));
            nextPlayer();
        } else {
            betHandler.PlayerBetMoney(playerBet, playerBet.GetMoney());
            nextPlayer();
        }
    }

    private void playerBetting(PokerPlayerProperties playerProperties) {
        PlayerBet playerBet = playerProperties.bet;

        if (playerBet.GetStake() < betHandler.CurrentMinBet) {
            if (playerBet.GetMoney() > (betHandler.CurrentMinBet - playerBet.GetStake())) {
                betHandler.PlayerBetMoney(playerBet, (betHandler.CurrentMinBet - playerBet.GetStake()));
            } else {
                betHandler.PlayerBetMoney(playerBet, playerBet.GetMoney());
                nextPlayer();
                return;
            }
        }

        playerProperties.State = PlayerPokerState.RAISING;
        setBetItemBar(playerProperties.playerNumber);
    }

    private boolean playerRaise(PokerPlayerProperties playerProperties, int amount) {
        if (betHandler.PlayerBetMoney(playerProperties.bet, amount)) {
            if (playerProperties.State != PlayerPokerState.RAISING) playerProperties.State = PlayerPokerState.RAISING;
            return true;
        } else return false;
    }

    private void playerFold(PokerPlayerProperties playerProperties) {
        playerProperties.State = PlayerPokerState.PREPARING;
        playerProperties.bet.ResetStake();
        setWaitingItemBar(playerProperties);

        if (playerOnTurn.equals(playerProperties.playerNumber)) nextPlayer();

        Inventory playerInventory = playerProperties.Player.getInventory();

        playerInventory.clear(0);
        playerInventory.clear(1);
    }

    private void setActionItemBar(PokerPlayerProperties playerProperties) {
        PlayerInventory playerInventory = playerList.get(playerProperties.playerNumber).Player.getInventory();
        clearHotBar(playerInventory);

        if (playerProperties.bet.GetStake() >= betHandler.CurrentMinBet) playerInventory.setItem(3, CHECKITEM);
        else playerInventory.setItem(3, CALLITEM);
        playerInventory.setItem(4, RAISEITEM);
        playerInventory.setItem(5, FOLDITEM);

        playerInventory.setItem(0, Card.getCardItem(playerProperties.hand.FirstCard));
        playerInventory.setItem(1, Card.getCardItem(playerProperties.hand.SecondCard));
    }

    private void setBetItemBar(int playerNumber) {
        PlayerInventory playerInventory = playerList.get(playerNumber).Player.getInventory();
        clearHotBar(playerInventory);

        for (int i = 0; i < 9; i++) if (betItemList.containsKey(i)) playerInventory.setItem(i, betItemList.get(i));
    }

    private void setWaitingItemBar(PokerPlayerProperties playerProperties) {
        PlayerInventory playerInventory = playerList.get(playerProperties.playerNumber).Player.getInventory();
        clearHotBar(playerInventory);

        playerInventory.setItem(0, LEAVEITEM);

        if (playerProperties.hand.FirstCard != null && playerProperties.hand.SecondCard != null) {
            playerInventory.setItem(0, Card.getCardItem(playerProperties.hand.FirstCard));
            playerInventory.setItem(1, Card.getCardItem(playerProperties.hand.SecondCard));
        }
    }

    private void clearHotBar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; i++) playerInventory.clear(i);
    }

    private void setPlayerMoneyToActionBar(PokerPlayerProperties playerProperties) {
        if (playerProperties == null) return;
        PlayerBet playerBet = playerProperties.bet;

        String message = "Geld: " + ChatColor.GOLD + playerBet.GetMoney() + ChatColor.WHITE + "  Einsatz: " + ChatColor.GOLD + playerBet.GetStake();
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

    private void initHologram() {
        Location location = new Location(joinBlockPosition.getWorld(), joinBlockPosition.getBlockX() + 0.5, joinBlockPosition.getBlockY() + 2.5, joinBlockPosition.getBlockZ() + 0.5);
        hologram = HologramsAPI.createHologram(ApatesCasino.getInstance(), location);

        hologram.insertTextLine(0, ChatColor.BLUE + "Poker: " + lobby.ID);
        potLine = hologram.insertTextLine(1, "Pot: ");
        cardLine = hologram.insertTextLine(2, "Karten: ");
        playerInformationLine = hologram.insertTextLine(3, "Am Zug: ");
    }

    private void updateHologram() {

        StringBuilder cards = new StringBuilder("| ");
        for (int i = 0; i < cardHandler.showedCards.size(); i++) {
            Card card = cardHandler.showedCards.get(i);
            cards.append(Card.GetTextCard(card)).append(" | ");
        }
        cardLine.setText("Karten: " + cards);
        potLine.setText("Pot: " + ChatColor.GOLD + betHandler.Pot + " | Mindesteinsatz: " + betHandler.CurrentMinBet);
        playerInformationLine.setText("Am Zug: " + ((playerOnTurn.equals(0) || !playerList.containsKey(playerOnTurn)) ? (ChatColor.AQUA + "Dealer") : playerList.get(playerOnTurn).Player.getDisplayName()));
    }

    // Actions after a player wrote a message
    public void OnPlayerSendMessage(UUID playerID, String message) {
        Player player = Bukkit.getPlayer(playerID);
        if (player == null) return;

        // Get unready player with the id and check if the message with the amount of money he will bring to the table is correct and add him to the game

        if (messageWaitingPlayers.contains(playerID) && message != null && message.matches("[0-9]+")) {

            // Check for acceptable amount of money
            int amount = Integer.parseInt(message);
            if (amount >= betHandler.MinMoney) {

                Economy econ = ApatesCasino.getEconomy();

                if (econ.getBalance(player) < betHandler.MinMoney) {
                    player.sendMessage(ChatColor.RED + "Du hast nicht genügend Geld um diesem Spiel beizutreten!");
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
                lobby.ChangePlayerState(playerID, PlayerState.READY);

                player.sendMessage(ChatColor.GREEN + "Du bist jetzt mit " + ChatColor.GOLD + amount + " Tokens" + ChatColor.GREEN + " im Spiel. Bitte warte auf die nächste Runde");

                PokerPlayerProperties playerProperties = getPlayerPropertiesByID(playerID);
                if (playerProperties != null) setWaitingItemBar(playerProperties);
            } else {
                player.sendMessage(ChatColor.RED + "Du must mindestens" + ChatColor.GOLD + betHandler.MinMoney + " Tokens" + ChatColor.RED + " zum Tisch mitbringen");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Bitte schreibe eine akzeptable Geldmenge, Der Mindesteinsatz lautet: " + ChatColor.GOLD + betHandler.MinMoney + " Tokens");
        }

        if (playerList.size() >= minPlayers && gameState == GameState.WAITFORPLAYERS) {
            gameState = GameState.STARTING;
            preparingTimer();
        }
    }

    public void PlayerAction(UUID playerID, int slot) {
        PokerPlayerProperties playerProperties = getPlayerPropertiesByID(playerID);
        if (playerProperties == null) return;
        Player player = playerProperties.Player;

        //System.out.println(playerProperties.playerNumber + " " + playerProperties.State.toString() + ": " + player.getDisplayName() + " " + slot);

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
                case 8:
                    RemovePlayer(playerProperties.playerID);
                    break;
            }
        } else if (playerProperties.State.equals(PlayerPokerState.RAISING) && gameState == GameState.ONGOING) {
            switch (slot) {
                case 0:
                    setWaitingItemBar(playerProperties);
                    nextPlayer();
                    break;
                case 2:
                    if (!playerRaise(playerProperties, betHandler.BetAmounts[0]))
                        player.sendMessage(ChatColor.RED + "Du hast nicht genügent Geld");
                    break;
                case 3:
                    if (!playerRaise(playerProperties, betHandler.BetAmounts[1]))
                        player.sendMessage(ChatColor.RED + "Du hast nicht genügent Geld");
                    break;
                case 4:
                    if (!playerRaise(playerProperties, betHandler.BetAmounts[2]))
                        player.sendMessage(ChatColor.RED + "Du hast nicht genügent Geld");
                    break;
                case 5:
                    if (!playerRaise(playerProperties, betHandler.BetAmounts[3]))
                        player.sendMessage(ChatColor.RED + "Du hast nicht genügent Geld");
                    break;
                case 6:
                    if (!playerRaise(playerProperties, betHandler.BetAmounts[4]))
                        player.sendMessage(ChatColor.RED + "Du hast nicht genügent Geld");
                    break;
                case 8:
                    if (!playerRaise(playerProperties, playerProperties.bet.GetMoney()))
                        player.sendMessage(ChatColor.RED + "Du hast nicht genügent Geld");
                    else nextPlayer();
                    break;
            }

            setPlayerMoneyToActionBar(playerProperties);
        } else {
            if (slot == 8) {
                RemovePlayer(playerID);
            }
            return;
        }

        updateHologram();
    }

    @Override
    public void StartGame() {
        if (gameState != GameState.STARTING) return;
        gameState = GameState.ONGOING;

        if (turnCounter != null) turnCounter.cancel();
        if (countDown != null) countDown.cancel();

        System.out.println("--- New Game ---");
        playerList.values().forEach(p -> System.out.println(p.Player.getDisplayName()));
        System.out.println("----");

        playerList.values().forEach(p -> p.Player.sendMessage(ChatColor.GREEN + "Die Runde startet jetzt"));

        playerOnTurn = 0;
        betHandler.Pot = 0;
        betHandler.sidePots = new HashMap<>();
        betHandler.CurrentMinBet = betHandler.BigBlind;

        cardHandler.InitGameCardHandler();

        // Give every player two cards and set the action-items
        for (PokerPlayerProperties playerProperty : playerList.values()) {
            playerProperty.State = PlayerPokerState.WAITING;

            cardHandler.InitPlayerPokerCards(playerProperty.hand);

            Player player = playerProperty.Player;
            Inventory playerInventory = player.getInventory();

            playerInventory.setItem(0, Card.getCardItem(playerProperty.hand.FirstCard));
            playerInventory.setItem(1, Card.getCardItem(playerProperty.hand.SecondCard));
            setWaitingItemBar(playerProperty);
            setPlayerMoneyToActionBar(playerProperty);
        }

        for (PokerPlayerProperties playerProperties : playerList.values())
            oldPlayerBalance.put(playerProperties.playerNumber, playerProperties.bet.GetMoney());

        if (getActivePlayers().size() > 1) {
            betHandler.SmallBlindPlayer = getNextActivePlayerNumber(betHandler.SmallBlindPlayer);
            if (betHandler.SmallBlindPlayer == 0) betHandler.SmallBlindPlayer = getNextActivePlayerNumber(0);
            betHandler.BigBlindPlayer = getNextActivePlayerNumber(betHandler.SmallBlindPlayer);
            if (betHandler.BigBlindPlayer == 0) betHandler.BigBlindPlayer = getNextActivePlayerNumber(0);

            betHandler.PlayerBetMoney(playerList.get(betHandler.SmallBlindPlayer).bet, betHandler.SmallBlind);
            betHandler.PlayerBetMoney(playerList.get(betHandler.BigBlindPlayer).bet, betHandler.BigBlind);
            playerOnTurn = betHandler.BigBlindPlayer;
            playerOnTurn = 1;
        }

        nextPlayer();
    }

    @Override
    public void CancelGame() {
        playerList.values().forEach(p -> RemovePlayer(p.playerID));

        hologram.delete();
        playerList.clear();
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
        Economy econ = ApatesCasino.getEconomy();

        if (econ.getBalance(player) < betHandler.MinMoney) {
            player.sendMessage(ChatColor.RED + "Du hast nicht genügend Geld üm diesem Spiel beizutreten!");
            return;
        }

        UUID playerID = player.getUniqueId();

        lobby.AddPlayer(player);
        lobby.ChangePlayerState(playerID, PlayerState.UNREADY);

        player.sendMessage(ChatColor.AQUA + "Bitte schreibe wie viel Geld du zum Tisch bringen willst");
        messageWaitingPlayers.add(playerID);
        writeMessageTimer(playerID);
    }

    @Override
    public void RemovePlayer(UUID playerID) {
        messageWaitingPlayers.remove(playerID);

        PokerPlayerProperties playerProperties = getPlayerPropertiesByID(playerID);
        if (playerProperties != null) {
            if (playerProperties.Player != null) {
                ApatesCasino.getEconomy().depositPlayer(playerProperties.Player, playerProperties.bet.GetMoney());
                clearHotBar(playerProperties.Player.getInventory());
                playerProperties.Player.sendMessage(ChatColor.RED + "Du hast die Runde verlassen");
            }

            playerList.remove(playerProperties.playerNumber);

            PlayerPokerState playerState = playerProperties.State;
            if (playerState.equals(PlayerPokerState.TURN) || playerState.equals(PlayerPokerState.RAISING)) nextPlayer();
        }

        lobby.RemovePlayer(playerID);
    }

    @Override
    public boolean containsPlayer(UUID playerID) {
        return lobby.getPlayer(playerID) != null;
    }
}