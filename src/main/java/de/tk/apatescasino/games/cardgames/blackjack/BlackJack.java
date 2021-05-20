package de.tk.apatescasino.games.cardgames.blackjack;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import de.tk.apatescasino.ApatesCasino;
import de.tk.apatescasino.bank.BankAccountHandler;
import de.tk.apatescasino.games.Game;
import de.tk.apatescasino.games.GameType;
import de.tk.apatescasino.games.ItemStackBuilder;
import de.tk.apatescasino.games.PlayerState;
import de.tk.apatescasino.games.cardgames.card.Card;
import de.tk.apatescasino.games.cardgames.card.CardDeck;
import de.tk.apatescasino.games.lobby.Lobby;
import de.tk.apatescasino.games.utilities.PlayerInventorySaver;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

enum BlackJackGameState {
    WAITING,
    PREPARING,
    ONGOING
}

public class BlackJack implements Game {

    private static final String BJ_PREFIX = String.format("%sBlackJack%s >> ", ChatColor.BLUE, ChatColor.WHITE);

    private final int minPlayers;
    private final int maxPlayers;
    private final Location joinBlockPosition;

    private final ItemStack HIT_ITEM = ItemStackBuilder.createItemStack(Material.PINK_DYE, 1, (ChatColor.DARK_GREEN + "HIT"), new String[]{"Hole eine weitere Karte"});
    private final ItemStack STAND_ITEM = ItemStackBuilder.createItemStack(Material.LIME_DYE, 1, (ChatColor.DARK_BLUE + "STAND"), new String[]{"Beende den Zug"});
    private final ItemStack LEAVE_ITEM = ItemStackBuilder.createItemStack(Material.RED_DYE, 1, (ChatColor.RED + "Leave"), new String[]{"Verlasse das Spiel"});

    private final Economy economy;
    private final BankAccountHandler bank;
    private final int minBet;
    private final int maxBet;

    private BlackJackGameState gameState;
    private Integer currentPlayer;
    private BukkitTask turnTimer;
    private final Integer turnTime;

    private final Integer preparingTime;
    private BukkitTask preparingTimer;


    private final Map<UUID, BlackJackPlayer> playerMap;
    private final Lobby lobby;

    private final CardDeck cardDeck;
    private final List<Card> croupierCards;
    private Integer croupierCardsValue;

    private final Hologram hologram;
    private final TextLine croupierLine;
    private final TextLine croupierCardsLine;
    private final TextLine playerLine;
    private final TextLine playerCardsLine;

    public BlackJack(String id, int minPlayers, int maxPlayers, Location joinBlockPosition, int minBet, int maxBet, int preparingTime, int turnTime) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.joinBlockPosition = joinBlockPosition;
        this.lobby = new Lobby(maxPlayers, minPlayers, id);
        this.bank = ApatesCasino.getBankAccountHandler();
        this.minBet = minBet;
        this.maxBet = maxBet;
        this.preparingTime = preparingTime;
        this.turnTime = turnTime;

        playerMap = new HashMap<>();
        cardDeck = new CardDeck();
        economy = ApatesCasino.getEconomy();
        croupierCards = new ArrayList<>();
        croupierCardsValue = 0;

        // initialize hologram
        Location location = new Location(joinBlockPosition.getWorld(), joinBlockPosition.getBlockX() + 0.5, joinBlockPosition.getBlockY() + 2.5, joinBlockPosition.getBlockZ() + 0.5);
        hologram = HologramsAPI.createHologram(ApatesCasino.getInstance(), location);

        hologram.insertTextLine(0, ChatColor.BLUE + "BlackJack: " + lobby.id);
        croupierLine = hologram.insertTextLine(1, ChatColor.DARK_PURPLE + "Croupier:");
        croupierCardsLine = hologram.insertTextLine(2, "");
        playerLine = hologram.insertTextLine(3, ChatColor.WHITE + "---");
        playerCardsLine = hologram.insertTextLine(4, "");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(ApatesCasino.getInstance(), () -> {
            for (BlackJackPlayer player : playerMap.values()) {
                setPlayerInformationBar(player);
            }
        }, 0L, 20L);

        bank.deposit(10000);

        waitingForPlayers();
    }


    private void startPreparingTimer() {
        if (gameState.equals(BlackJackGameState.PREPARING)) return;
        else gameState = BlackJackGameState.PREPARING;

        preparingTimer = new BukkitRunnable() {
            int timer = preparingTime;

            @Override
            public void run() {
                if (timer == preparingTime) {
                    for (BlackJackPlayer player : playerMap.values()) {
                        lobby.changePlayerState(player.Player.getUniqueId(), PlayerState.UNREADY);
                    }
                }

                if (timer <= 0) {
                    // Inform all player which didn't bet and remove them from the game
                    for (BlackJackPlayer player : playerMap.values().stream().filter(p -> p.getStake() == 0).collect(Collectors.toList())) {
                        player.Player.sendMessage(BJ_PREFIX + ChatColor.RED + "Sie haben leider keinen Einsatz platziert");
                        removePlayer(player.Player.getUniqueId());
                    }

                    // Start game if enough players are in the lobby, else wait for them
                    if (playerMap.values().size() >= minPlayers) startNewGame();
                    else waitingForPlayers();

                    preparingTimer.cancel();
                } else {
                    timer--;
                    playerLine.setText(ChatColor.YELLOW.toString() + timer + " Sekunden bis zum Start");
                }
            }
        }.runTaskTimer(ApatesCasino.getInstance(), 0, 20L);
    }

    private void startTurnTimer(int seconds) {
        turnTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentPlayer != 0) {
                    BlackJackPlayer player = getPlayerByNumber(currentPlayer);
                    if (player != null) {
                        player.Player.sendMessage(BJ_PREFIX + ChatColor.RED + "Ihre Zeit ist um und Ihr Zug beendet");
                        playerStand();
                    }
                }
                nextPlayer();
            }
        }.runTaskLater(ApatesCasino.getInstance(), (seconds * 20L));
    }

    private void startNewGame() {
        gameState = BlackJackGameState.ONGOING;

        croupierCards.clear();
        croupierCardsValue = 0;

        // Set player states and write starting message
        for (BlackJackPlayer player : playerMap.values()) {
            player.resetCards();
            clearHotBar(player);

            player.state = BlackJackPlayerState.IN_GAME;
            lobby.changePlayerState(player.Player.getUniqueId(), PlayerState.INGAME);
            player.Player.sendMessage(BJ_PREFIX + ChatColor.GREEN + "Das Spiel hat nun begonnen");
        }

        // Initialize Deck
        cardDeck.InitStandardDeck();
        cardDeck.ShuffleDeck();

        // Draw Cards
        for (BlackJackPlayer player : playerMap.values()) player.addCard(cardDeck.pickFirst());
        croupierCards.add(cardDeck.pickFirst());
        croupierCardsValue = BlackJackPlayer.getCalculatedCardsValue(croupierCards);
        for (BlackJackPlayer player : playerMap.values()) player.addCard(cardDeck.pickFirst());

        currentPlayer = 0;
        nextPlayer();
    }

    private void nextPlayer() {
        if (turnTimer != null) turnTimer.cancel();
        System.out.println("next Player");
        // Get current player and set waiting properties
        BlackJackPlayer player = getPlayerByNumber(currentPlayer);
        if (player != null && !player.state.equals(BlackJackPlayerState.PREPARING)) {
            player.state = BlackJackPlayerState.IN_GAME;
            clearHotBar(player);
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

        updateHologram();
    }

    private void playerTurn(BlackJackPlayer player) {

        currentPlayer = player.PlayerNumber;
        player.state = BlackJackPlayerState.BETTING;
        setBettingBar(player);

        player.Player.sendMessage(BJ_PREFIX + ChatColor.YELLOW + "Sie sind nun am Zug und haben " + turnTime + " Sekunden Zeit");

        startTurnTimer(turnTime);
    }

    private void croupierTurn() {
        croupierCards.add(cardDeck.pickFirst());
        croupierCardsValue = BlackJackPlayer.getCalculatedCardsValue(croupierCards);

        if (croupierCardsValue < 17) {
            startTurnTimer(3);
        } else {
            endGame();
        }
    }

    private void endGame() {
        turnTimer.cancel();

        for (BlackJackPlayer player : getActivePlayers()) {

            if (player.getCardsValue() > croupierCardsValue || croupierCardsValue > 21) {
                economy.depositPlayer(player.Player, player.getStake());
                bank.transferToPlayer(player.Player, player.getStake());

                player.Player.sendMessage(BJ_PREFIX + ChatColor.GREEN + "Sie haben den Croupier überboten und "
                        + ChatColor.GOLD + player.getStake() * 2 + " Adonen" + ChatColor.GREEN + " gewonen");

            } else if (player.getCardsValue().equals(croupierCardsValue)) {
                economy.depositPlayer(player.Player, player.getStake());
                player.Player.sendMessage(BJ_PREFIX + ChatColor.YELLOW + "Sie sind mit dem Croupier gleich und bekommen ihre "
                        + ChatColor.GOLD + player.getStake() + " Adonen" + ChatColor.YELLOW + " zurück");

            } else {
                bank.deposit(player.getStake());
                player.resetStake();
                player.state = BlackJackPlayerState.PREPARING;

                player.Player.sendMessage(BJ_PREFIX + ChatColor.RED + "Der Croupier verfügt über bessere Karten, sie haben ihren Einsatz somit verloren");
            }

            player.resetStake();
        }

        if (playerMap.values().size() >= minPlayers) {
            for (BlackJackPlayer player : playerMap.values()) {
                UUID playerID = player.Player.getUniqueId();

                player.resetStake();
                player.Player.sendMessage(BJ_PREFIX + ChatColor.GREEN + "Die runde ist beendet und startet erneut");

                lobby.changePlayerState(playerID, PlayerState.UNREADY);

                waitForPlayerBetMessage(player.Player);
                setWaitingBar(player);
            }

            startPreparingTimer();
        } else {
            waitingForPlayers();
        }
    }

    private void waitingForPlayers() {
        gameState = BlackJackGameState.WAITING;

        croupierLine.setText(ChatColor.WHITE + "-----");
        croupierCardsLine.setText("");
        playerLine.setText(ChatColor.YELLOW + "Zurzeit kein Spiel im Gange");
        playerCardsLine.setText("");
    }

    public void PlayerAction(UUID playerID, int slot) {
        if (!playerMap.containsKey(playerID)) return;
        BlackJackPlayer player = playerMap.get(playerID);

        if (player.state.equals(BlackJackPlayerState.PREPARING)) {
            switch (slot) {
                case 8:
                    removePlayer(playerID);
                    break;
            }
        } else if (player.state.equals(BlackJackPlayerState.BETTING)) {
            switch (slot) {
                case 4:
                    playerHit(player);
                    break;
                case 5:
                    playerStand();
            }
        }
    }

    private void playerHit(BlackJackPlayer player) {
        player.addCard(cardDeck.pickFirst());

        if (player.getCardsValue() > 21) {
            clearHotBar(player);

            bank.deposit(player.getStake());
            player.resetStake();
            player.state = BlackJackPlayerState.PREPARING;

            setWaitingBar(player);
            player.Player.sendMessage(BJ_PREFIX + ChatColor.RED + "Sie haben sich überkauft, Ihr Geld geht and den Croupier");

            nextPlayer();
        }

        updateHologram();
    }

    private void playerStand() {
        nextPlayer();
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

    private void setPlayerInformationBar(BlackJackPlayer player) {
        if (player == null || player.getCards().size() <= 0) return;

        String message = Card.getCardsAsText(player.getCards()) + " - Kartenwert: " + player.getCardsValue();
        player.Player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
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

    private void updateHologram() {
        if (!gameState.equals(BlackJackGameState.WAITING)) {
            croupierLine.setText(ChatColor.RED + "Croupier: ");
            croupierCardsLine.setText(Card.getCardsAsText(croupierCards) + ChatColor.WHITE + " - Kartenwert: " + croupierCardsValue);


            BlackJackPlayer player = getPlayerByNumber(currentPlayer);
            if (player != null) {
                playerLine.setText(ChatColor.WHITE + "Spieler:");
                playerLine.setText(ChatColor.WHITE + "Spieler: " + ChatColor.AQUA + player.Player.getDisplayName() + ChatColor.WHITE + " Einsatz - " + ChatColor.GOLD + player.getStake());
                playerCardsLine.setText(Card.getCardsAsText(player.getCards()) + ChatColor.WHITE + " - Kartenwert: " + player.getCardsValue());
            } else {
                playerLine.setText(ChatColor.WHITE + "---");
                playerCardsLine.setText("");
            }
        } else {
            croupierLine.setText(ChatColor.WHITE + "-----");
            croupierCardsLine.setText("");
            playerLine.setText("");
            playerCardsLine.setText("");
        }
    }

    // Actions after a player wrote a message
    private void waitForPlayerBetMessage(Player player) {
        BlackJackPlayer blackJackPlayer = playerMap.get(player.getUniqueId());
        if (blackJackPlayer == null) return;

        player.sendMessage(BJ_PREFIX + ChatColor.AQUA + "Bitte platzieren sie Ihre Wette (MIN: " + ChatColor.GOLD + minBet + ChatColor.AQUA +
                " | MAX: " + ChatColor.GOLD + maxBet + ChatColor.AQUA + ")");


        ApatesCasino.getChatMessageHandler().addExpectedMessage(player, message -> {
            if (gameState == BlackJackGameState.PREPARING && blackJackPlayer.getStake() == 0 && message != null && message.matches("[0-9]+")) {

                // Check for acceptable amount of money
                int amount = Integer.parseInt(message);
                if (amount >= minBet && amount <= maxBet) {

                    if (economy.getBalance(blackJackPlayer.Player) < amount) {
                        blackJackPlayer.Player.sendMessage(BJ_PREFIX + ChatColor.RED + "Sie haben nicht genügend Geld um diese Wette zu platzieren!");
                        waitForPlayerBetMessage(player);
                    } else {
                        economy.withdrawPlayer(blackJackPlayer.Player, amount);
                        blackJackPlayer.addMoneyToStake(amount);

                        blackJackPlayer.Player.sendMessage(BJ_PREFIX + ChatColor.GREEN + "Sie sind nun mit einem Einsatz von " + ChatColor.GOLD + blackJackPlayer.getStake() + " Tokens" +
                                ChatColor.GREEN + " in der Runde");

                        lobby.changePlayerState(player.getUniqueId(), PlayerState.READY);
                    }
                } else {
                    player.sendMessage(BJ_PREFIX + ChatColor.RED + "Geben sie bitte einen passenden Betrag ein (MIN: " + ChatColor.GOLD + minBet + ChatColor.AQUA +
                            " | MAX: " + ChatColor.GOLD + maxBet + ChatColor.AQUA + ")");

                    waitForPlayerBetMessage(player);
                }
            } else {
                player.sendMessage(BJ_PREFIX + ChatColor.RED + "Geben sie bitte einen passenden Betrag ein (MIN: " + ChatColor.GOLD + minBet + ChatColor.AQUA +
                        " | MAX: " + ChatColor.GOLD + maxBet + ChatColor.AQUA + ")");

                waitForPlayerBetMessage(player);
            }
        });
    }

    @Override
    public void startGame() {

    }

    @Override
    public void cancelGame() {
        for (BlackJackPlayer player : playerMap.values()) {
            removePlayer(player.Player.getUniqueId());
        }

        hologram.delete();
        playerMap.clear();
    }

    @Override
    public GameType getGameType() {
        return GameType.BLACKJACK;
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
    public void addPlayer(Player player) {
        UUID playerID = player.getUniqueId();

        // Reject player to specific conditions
        if (gameState.equals(BlackJackGameState.ONGOING)) {
            player.sendMessage(BJ_PREFIX + "Bitte warten Sie bis zur Vorbereitungsphase, bevor sie diesem Spiel erneut beitreten");
            return;
        } else if (playerMap.containsKey(playerID)) {
            player.sendMessage(BJ_PREFIX + ChatColor.YELLOW + "Sie sind bereits mitglied dieses Spiels");
            return;
        } else if (playerMap.size() >= maxPlayers) {
            player.sendMessage(BJ_PREFIX + ChatColor.RED + "Das Spiel besitz bereits die maximale Anzahl an Spielern");
            return;
        } else if (economy.getBalance(player) <= minBet) {
            player.sendMessage(BJ_PREFIX + ChatColor.RED + "Sie haben leider nicht genügend Geld, um diesem Spiel beizutreten");
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

        // Send failure message
        if (playerNumber == 0) {
            player.sendMessage(BJ_PREFIX + ChatColor.RED + "Beim betreten dieses Spiels, sind unerwartete Fehler aufgetreten");
        }

        // Add player to lobby
        lobby.addPlayer(player);
        lobby.changePlayerState(playerID, PlayerState.UNREADY);

        PlayerInventorySaver.addPlayerInventory(player);

        // Create Player
        playerMap.put(playerID, new BlackJackPlayer(player, playerNumber));
        player.sendMessage(BJ_PREFIX + ChatColor.GREEN + "Sie sind nun mitglied dieses Spiels");

        waitForPlayerBetMessage(player);

        if (gameState.equals(BlackJackGameState.WAITING) && playerMap.values().size() >= minPlayers) {
            startPreparingTimer();
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                BlackJackPlayer blackJackPlayer = playerMap.get(playerID);
                if (blackJackPlayer != null && blackJackPlayer.state == BlackJackPlayerState.PREPARING) {
                    setWaitingBar(playerMap.get(playerID));
                }
            }
        }.runTaskLater(ApatesCasino.getInstance(), 2 * 20L);


    }

    @Override
    public void removePlayer(UUID playerID) {
        BlackJackPlayer player = playerMap.get(playerID);
        if (player != null) {
            if (player.state != BlackJackPlayerState.PREPARING) {
                bank.deposit(player.getStake());
                if (player.state.equals(BlackJackPlayerState.BETTING)) nextPlayer();
            } else {
                economy.depositPlayer(player.Player, player.getStake());
            }
            player.resetStake();

            player.Player.sendMessage(BJ_PREFIX + ChatColor.RED + "Sie haben das Spiel verlassen!");
            clearHotBar(player);

            PlayerInventorySaver.setPlayerInventory(player.Player);
        }

        playerMap.remove(playerID);
        lobby.removePlayer(playerID);
    }

    @Override
    public boolean containsPlayer(UUID playerID) {
        return playerMap.containsKey(playerID);
    }

    public static Game createGame(BlackJackConfig config) {
        return new BlackJack(config.gameID, config.minPlayers, config.maxPlayers, config.joinBlockPosition.getLocation(),
                    config.minBet, config.maxBet, config.preparingTime, config.turnTime);
    }
}
