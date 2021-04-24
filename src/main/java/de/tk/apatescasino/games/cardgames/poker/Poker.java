package de.tk.apatescasino.games.cardgames.poker;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import de.tk.apatescasino.ApatesCasino;
import de.tk.apatescasino.games.*;
import de.tk.apatescasino.games.cardgames.card.Card;
import de.tk.apatescasino.games.utilities.PlayerBet;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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

enum ActionItem {
    CHECK,
    CALL,
    RAISE,
    FOLD,
    LEAVE
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

    private final Location joinBlockPosition;

    // Lobby which holds all the players of the game lobby
    private final Lobby lobby;
    private final PokerCardHandler cardHandler;
    private final PokerBetHandler betHandler;

    // Players to be waited for a message
    private final List<UUID> messageWaitingPlayers;
    // All players with their properties which are in the game
    private final Map<Integer, PokerPlayerProperties> playerList;

    // Items which are used to raise the stake by a specific amount
    private final Map<Integer, ItemStack> betItemList;

    private GameState gameState;
    private Integer playerOnTurn;
    private BukkitTask turnCounter;

    private Hologram hologram;
    private TextLine potLine;
    private TextLine cardLine;
    private TextLine playerInformationLine;


    public Poker(String name, Location joinBlockPosition, int smallBlind, int bigBlind, int minMoney, int minPlayers, int maxPlayers) {
        messageWaitingPlayers = new ArrayList<>();
        playerList = new HashMap<>();

        // Initialize game-settings
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.joinBlockPosition = joinBlockPosition;

        // Initialize lobby for the players
        this.lobby = new Lobby(minPlayers, maxPlayers, name);
        cardHandler = new PokerCardHandler();
        this.betHandler = new PokerBetHandler(minMoney, smallBlind, bigBlind);

        gameState = GameState.WAITFORPLAYERS;

        // Initialize bet items
        betItemList = new HashMap<Integer, ItemStack>() {{
            put(0, ItemStackBuilder.createItemStack(Material.BARRIER, 1, (ChatColor.DARK_RED + "Cancel"), new String[]{"Erhöhen abbrechen"}));
            put(2, ItemStackBuilder.createItemStack(Material.COAL, 1, (ChatColor.DARK_GRAY + betHandler.BetAmounts[0].toString() + " Tokens"), new String[]{"Erhöhe deinen Einsatz um " + betHandler.BetAmounts[0].toString() + " Tokens"}));
            put(3, ItemStackBuilder.createItemStack(Material.IRON_INGOT, 1, (ChatColor.AQUA + betHandler.BetAmounts[1].toString() + " Tokens"), new String[]{"Erhöhe deinen Einsatz um " + betHandler.BetAmounts[1].toString() + " Tokens"}));
            put(4, ItemStackBuilder.createItemStack(Material.GOLD_INGOT, 1, (ChatColor.DARK_GREEN + betHandler.BetAmounts[2].toString() + " Tokens"), new String[]{"Erhöhe deinen Einsatz um" + betHandler.BetAmounts[2].toString() + " Tokens"}));
            put(5, ItemStackBuilder.createItemStack(Material.DIAMOND, 1, (ChatColor.LIGHT_PURPLE + betHandler.BetAmounts[3].toString() + " Tokens"), new String[]{"Erhöhe deinen Einsatz um " + betHandler.BetAmounts[3].toString() + " Tokens"}));
            put(6, ItemStackBuilder.createItemStack(Material.EMERALD, 1, (ChatColor.DARK_PURPLE + betHandler.BetAmounts[4].toString() + " Tokens"), new String[]{"Erhöhe deinen Einsatz um " + betHandler.BetAmounts[4].toString() + " Tokens"}));
            put(8, ItemStackBuilder.createItemStack(Material.NETHER_STAR, 1, (ChatColor.GOLD + "All In"), new String[]{"Erhöhe deinen Einsatz um all deine Tokens"}));
        }};

        Bukkit.getScheduler().scheduleSyncRepeatingTask(ApatesCasino.getInstance(), () -> {
            for (PokerPlayerProperties playerProperties : playerList.values()) {
                setPlayerMoneyToActionBar(playerProperties);
            }
        }, 0L, 20L);

        initHologram();
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
                                if (player != null)
                                    player.sendMessage(ChatColor.RED + "Sorry du hast zu lange gebraucht, du hast das Spiel verlassen");
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
        scheduler.scheduleSyncDelayedTask(ApatesCasino.getInstance(), () -> {
            switch (type) {
                case "preparing":
                    if (playerList.size() < minPlayers) {
                        gameState = GameState.WAITFORPLAYERS;
                    } else StartGame();

                    break;
                default:
                    break;
            }
        }, delayInSec * 20L);
    }

    private void startTurnTime(int playerNumber, int delayInSec) {
        turnCounter = new BukkitRunnable() {
            @Override
            public void run() {
                if (playerNumber != 0) {
                    playerList.get(playerNumber).Player.sendMessage(ChatColor.RED + "Deine Zeit um, dein Zug ist beendet");
                    //playerFold(playerNumber);
                }
                nextPlayer();
            }
        }.runTaskLater(ApatesCasino.getInstance(), (delayInSec * 20L));
    }


    // Turn to the next player
    private void nextPlayer() {
        if (turnCounter != null) turnCounter.cancel();

        if (playerOnTurn != 0) {
            setActionItemBar(playerOnTurn);
            playerList.get(playerOnTurn).State = PlayerPokerState.WAITING;
        }
        updateHologram();

        if (getActivePlayers().size() == 1) {
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
        PokerPlayerProperties player = playerList.get(playerOnTurn);

        player.State = PlayerPokerState.TURN;
        player.Player.sendMessage(ChatColor.GREEN + "Du bist am Zug");
        startTurnTime(playerOnTurn, 30);

        updateHologram();
    }

    private void dealerTurn() {
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
        for (PokerPlayerProperties playerProperties : playerList.values()) playerProperties.bet.ResetStake();
        turnCounter.cancel();

        List<PokerPlayerProperties> winners = cardHandler.getWinners(getActivePlayers());

        String endMessage = cardHandler.GetEndMessage(getActivePlayers(), winners);
        playerList.values().forEach(p -> p.Player.sendMessage(endMessage));

        for (PokerPlayerProperties player : winners) player.bet.AddMoney(betHandler.Pot / winners.size());

        for (PokerPlayerProperties playerProperties : playerList.values())
            playerProperties.State = PlayerPokerState.PREPARING;

        waitForGame("preparing", 5);
        gameState = GameState.STARTING;
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

    private boolean playerRaise(int playerNumber, int amount) {
        PokerPlayerProperties player = playerList.get(playerNumber);
        if (betHandler.PlayerBetMoney(player.bet, amount)) {
            if (player.State != PlayerPokerState.RAISING) player.State = PlayerPokerState.RAISING;
            return true;
        } else return false;
    }

    private void playerFold(int playerNumber) {
        PokerPlayerProperties player = playerList.get(playerNumber);
        player.State = PlayerPokerState.PREPARING;
        player.bet.ResetStake();

        Inventory playerInventory = player.Player.getInventory();

        playerInventory.clear(0);
        playerInventory.clear(1);
    }

    private void setActionItemBar(int playerNumber) {
        PokerPlayerProperties playerProperty = playerList.get(playerNumber);
        if (playerProperty == null) return;

        PlayerInventory playerInventory = playerList.get(playerNumber).Player.getInventory();
        clearHotBar(playerInventory);

        if (playerProperty.bet.GetStake() >= betHandler.CurrentMinBet) playerInventory.setItem(3, CHECKITEM);
        else playerInventory.setItem(3, CALLITEM);
        playerInventory.setItem(4, RAISEITEM);
        playerInventory.setItem(5, FOLDITEM);
        playerInventory.setItem(8, LEAVEITEM);

        playerInventory.setItem(0, Card.getCardItem(playerProperty.hand.FirstCard));
        playerInventory.setItem(1, Card.getCardItem(playerProperty.hand.SecondCard));
    }

    private void setBetItemBar(int playerNumber) {
        PlayerInventory playerInventory = playerList.get(playerNumber).Player.getInventory();
        clearHotBar(playerInventory);

        for (int i = 0; i < 9; i++) if (betItemList.containsKey(i)) playerInventory.setItem(i, betItemList.get(i));
    }

    private void clearHotBar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; i++) playerInventory.clear(i);
    }

    private void setPlayerMoneyToActionBar(PokerPlayerProperties playerProperties) {
        if (playerProperties == null) return;
        PlayerBet playerBet = playerProperties.bet;

        String message = "Geld: " + playerBet.GetMoney() + "  Einsatz: " + playerBet.GetStake();
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
            if (playerList.containsKey(number) && playerList.get(number).State != PlayerPokerState.PREPARING)
                return number;
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
        playerInformationLine.setText("Am Zug: " + (playerOnTurn.equals(0) ? (ChatColor.AQUA + "Dealer") : playerList.get(playerOnTurn).Player.getDisplayName()));
    }

    // Actions after a player wrote a message
    public void OnPlayerSendMessage(UUID playerID, String message) {
        Player player = Bukkit.getPlayer(playerID);
        if (player == null) return;

        // Get unready player with the id and check if the message with the amount of money he will bring to the table is correct and add him to the game
        for (LobbyPlayer lobbyPlayer : lobby.getPlayersByState(PlayerState.UNREADY)) {
            if (messageWaitingPlayers.contains(playerID) && message != null && message.matches("[0-9]+")) {

                // Check for acceptable amount of money
                int amount = Integer.parseInt(message);
                if (amount >= betHandler.MinMoney) {

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
                    if (playerList.size() >= minPlayers) {
                        gameState = GameState.STARTING;
                        waitForGame("preparing", 20);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Du must mindestens" + ChatColor.GOLD + betHandler.MinMoney + " Tokens" + ChatColor.RED + " zum Tisch mitbringen");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Bitte schreibe eine akzeptable Geldmenge, Der Mindesteinsatz lautet: " + ChatColor.GOLD + betHandler.MinMoney + " Tokens");
            }
        }

        if (lobby.getPlayersByState(PlayerState.READY).size() > minPlayers) {
            gameState = GameState.STARTING;
            waitForGame("preparing", 10);
        }
    }

    public void PlayerAction(UUID playerID, int slot) {
        PokerPlayerProperties playerProperties = getPlayerPropertiesByID(playerID);
        if (playerProperties == null) return;
        Player player = playerProperties.Player;

        //System.out.println(playerProperties.playerNumber + " " + playerProperties.State.toString() + ": " + player.getDisplayName() + " " + slot);

        if (playerProperties.State.equals(PlayerPokerState.TURN)) {
            switch (slot) {
                case 3:
                    playerCheckCall(playerProperties);
                    break;
                case 4:
                    playerProperties.State = PlayerPokerState.RAISING;
                    setBetItemBar(playerProperties.playerNumber);
                    break;
                case 5:
                    playerFold(playerProperties.playerNumber);
                    break;
                case 8:
                    RemovePlayer(playerProperties.playerID);
                    break;
            }
        } else if (playerProperties.State.equals(PlayerPokerState.RAISING)) {
            switch (slot) {
                case 0:
                    playerProperties.State = PlayerPokerState.TURN;
                    setActionItemBar(playerProperties.playerNumber);
                    break;
                case 2:
                    if (!playerRaise(playerProperties.playerNumber, betHandler.BetAmounts[0]))
                        player.sendMessage(ChatColor.RED + "Du hast nicht genügent Geld");
                    break;
                case 3:
                    if (!playerRaise(playerProperties.playerNumber, betHandler.BetAmounts[1]))
                        player.sendMessage(ChatColor.RED + "Du hast nicht genügent Geld");
                    break;
                case 4:
                    if (!playerRaise(playerProperties.playerNumber, betHandler.BetAmounts[2]))
                        player.sendMessage(ChatColor.RED + "Du hast nicht genügent Geld");
                    break;
                case 5:
                    if (!playerRaise(playerProperties.playerNumber, betHandler.BetAmounts[3]))
                        player.sendMessage(ChatColor.RED + "Du hast nicht genügent Geld");
                    break;
                case 6:
                    if (!playerRaise(playerProperties.playerNumber, betHandler.BetAmounts[4]))
                        player.sendMessage(ChatColor.RED + "Du hast nicht genügent Geld");
                    break;
                case 8:
                    if (!playerRaise(playerProperties.playerNumber, playerProperties.bet.GetMoney()))
                        player.sendMessage(ChatColor.RED + "Du hast nicht genügent Geld");
                    break;
            }

            setPlayerMoneyToActionBar(playerProperties);
        } else {
            if (slot == 8) {
                RemovePlayer(playerID);
            } else {
                player.sendMessage(ChatColor.RED + "Es ist nicht dein Zug");
            }
            return;
        }

        updateHologram();
    }

    @Override
    public void StartGame() {

        if (gameState != GameState.STARTING) return;
        gameState = GameState.ONGOING;

        playerOnTurn = 0;
        betHandler.Pot = 0;
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
            setActionItemBar(playerProperty.playerNumber);
            setPlayerMoneyToActionBar(playerProperty);
        }
        //betHandler.SmallBlindPlayer = getNextActivePlayerNumber(betHandler.SmallBlindPlayer);
        //betHandler.BigBlindPlayer = getNextActivePlayerNumber(betHandler.SmallBlindPlayer);
        //betHandler.PlayerBetMoney(playerList.get(betHandler.SmallBlindPlayer).bet, betHandler.SmallBlind);
        //betHandler.PlayerBetMoney(playerList.get(betHandler.BigBlindPlayer).bet, betHandler.BigBlind);
        //playerOnTurn = betHandler.BigBlindPlayer;
        playerOnTurn = 1;
        nextPlayer();
    }

    @Override
    public void CancelGame() {
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
        UUID playerID = player.getUniqueId();

        lobby.AddPlayer(player);
        lobby.ChangePlayerState(playerID, PlayerState.UNREADY);

        player.sendMessage(ChatColor.AQUA + "Bitte schreibe wie viel Geld du zum Tisch bringen willst");
        messageWaitingPlayers.add(playerID);
        waitForPlayer("setMoney", playerID, 20);
    }

    @Override
    public void RemovePlayer(UUID playerID) {
        messageWaitingPlayers.remove(playerID);

        PokerPlayerProperties pokerPlayerProperties = getPlayerPropertiesByID(playerID);
        if (pokerPlayerProperties != null) {

            playerList.remove(pokerPlayerProperties.playerNumber);
        }

        lobby.RemovePlayer(playerID);
    }

    @Override
    public boolean containsPlayer(UUID playerID) {
        return lobby.getPlayer(playerID) != null;
    }
}