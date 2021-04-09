package de.tk.apatescasino.games.cardgames;


import de.tk.apatescasino.ApatesCasino;
import de.tk.apatescasino.games.*;
import org.apache.commons.lang.ArrayUtils;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private enum PokerHand {
        HIGH_CARD,
        PAIR,
        TWO_PAIR,
        THREE_OF_A_KIND,
        STRAIGHT,
        FLUSH,
        FULL_HOUSE,
        FOUR_OF_A_KIND,
        STRAIGHT_FLUSH,
        ROYAL_FLUSH
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
        public PokerHand Hand;
        public Integer HandScore;
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
        for (PlayerProperties player : players) calculatePlayerHand(player.playerNumber);
        players.sort(Comparator.comparing(p -> p.Hand.ordinal()));
        Collections.reverse(players);

        List<PlayerProperties> potentialPlayers = players.stream().filter(h -> h.Hand.ordinal() == players.get(0).Hand.ordinal()).sorted(Comparator.comparing(s -> s.HandScore)).collect(Collectors.toList());
        Collections.reverse(potentialPlayers);
        List<PlayerProperties> highestScorePlayers = potentialPlayers.stream().filter(p -> p.HandScore.equals(potentialPlayers.get(0).HandScore)).collect(Collectors.toList());

        List<PlayerProperties> highestFirstCardPlayers = highestScorePlayers.stream().filter(c -> c.FirstCard.Rank.ordinal() == highestScorePlayers.get(0).FirstCard.Rank.ordinal()).sorted(Comparator.comparing(c -> c.FirstCard.Rank.ordinal())).collect(Collectors.toList());
        Collections.reverse(highestFirstCardPlayers);
        List<PlayerProperties> highestSecondCardPlayers = highestScorePlayers.stream().filter(c -> c.SecondCard.Rank.ordinal() == highestScorePlayers.get(0).SecondCard.Rank.ordinal()).sorted(Comparator.comparing(c -> c.SecondCard.Rank.ordinal())).collect(Collectors.toList());
        Collections.reverse(highestSecondCardPlayers);

        List<PlayerProperties> winners = new ArrayList<>();

        if (highestFirstCardPlayers.get(0).FirstCard.Rank.ordinal() >= highestSecondCardPlayers.get(0).SecondCard.Rank.ordinal()) {
            winners.addAll(highestFirstCardPlayers.stream().filter(p -> p.FirstCard.Rank.ordinal() == highestFirstCardPlayers.get(0).FirstCard.Rank.ordinal()).collect(Collectors.toList()));
        }
        if (highestFirstCardPlayers.get(0).FirstCard.Rank.ordinal() <= highestSecondCardPlayers.get(0).SecondCard.Rank.ordinal()){
            winners.addAll(highestSecondCardPlayers.stream().filter(p -> p.SecondCard.Rank.ordinal() == highestSecondCardPlayers.get(0).SecondCard.Rank.ordinal()).collect(Collectors.toList()));
        }

        for (PlayerProperties player : winners) {
            player.AddMoney(pot / winners.size());
        }
    }

    private boolean playerCheck(int playerNumber) {
        PlayerProperties player = playerList.get(playerNumber);
        if (player.getStake().equals(currentMinBet)) {
            nextPlayer();
            return true;
        }
        else return false;
    }
    private boolean playerCall(int playerNumber) {
        PlayerProperties player = playerList.get(playerNumber);

        if (player.getStake() < currentMinBet && playerBetMoney(playerNumber, (currentMinBet - player.getStake()))) {
            nextPlayer();
            return true;
        } else return false;
    }
    private boolean playerRaise(int playerNumber, int amount) {
        PlayerProperties player = playerList.get(playerNumber);
        if (playerBetMoney(playerNumber ,amount)){
            if (player.State != PlayerPokerState.RAISING) player.State = PlayerPokerState.RAISING;
            return true;
        } else return false;
    }

    private void calculatePlayerHand(int playerNumber) {
        PlayerProperties player = playerList.get(playerNumber);
        if (player == null) return;

        List<Card> allCards = new ArrayList<>(showedCards);
        allCards.add(player.FirstCard);
        allCards.add(player.SecondCard);
        allCards.sort(Comparator.comparing(c -> c.Rank.ordinal()));

        List<List<Card>> sameTypeCards = getSameTypeCards(allCards);
        List<List<Card>> sameRankCards = getSameRankCards(allCards);

        List<Card> finalCards;

        // Royal FLush
        if (sameTypeCards.stream().anyMatch(l -> l.size() >= 5)) {
            List<Card> cards = sameTypeCards.stream().filter(l -> l.size() >= 5).findFirst().orElse(new ArrayList<>());
            cards.sort(Comparator.comparing(c -> c.Rank.ordinal()));

            finalCards = new ArrayList<>();
            int index = CardRank.TEN.ordinal();

            for (Card card : cards) {
                if (card.Rank.ordinal() == index) {
                    index++;
                    finalCards.add(card);
                }
            }
            if (index > CardRank.ACE.ordinal()) {
                player.HandScore = getHandValue(finalCards, PokerHand.ROYAL_FLUSH);
                player.Hand = PokerHand.ROYAL_FLUSH;
                return;
            }
        }

        // Straight Flush
        if (sameTypeCards.stream().anyMatch(l -> l.size() >= 5)) {
            List<Card> cards = sameTypeCards.stream().filter(l -> l.size() >= 5).findFirst().orElse(new ArrayList<>());

            finalCards = getHighestStraight(cards);
            if (finalCards != null) {
                player.HandScore = getHandValue(finalCards, PokerHand.STRAIGHT_FLUSH);
                player.Hand = PokerHand.STRAIGHT_FLUSH;
                return;
            }
        }

        // Four of a kind
        if (sameRankCards.stream().anyMatch(l -> l.size() == 4)) {
            List<Card> cards = sameRankCards.stream().filter(l -> l.size() == 4).findFirst().orElse(new ArrayList<>());

            player.HandScore = getHandValue(cards, PokerHand.FOUR_OF_A_KIND);
            player.Hand = PokerHand.FOUR_OF_A_KIND;
            return;
        }

        // Full House
        if (sameRankCards.stream().anyMatch(l -> l.size() == 3) && sameRankCards.stream().anyMatch(l -> l.size() == 2)) {
            List<List<Card>> threeOfAKind = sameRankCards.stream().filter(l -> l.size() == 3).collect(Collectors.toList());
            List<List<Card>> pairs = sameRankCards.stream().filter(l -> l.size() == 2).collect(Collectors.toList());

            threeOfAKind.sort(Comparator.comparing(l -> l.get(0).Rank.ordinal()));
            pairs.sort(Comparator.comparing(l -> l.get(0).Rank.ordinal()));

            finalCards = threeOfAKind.get(0);
            finalCards.addAll(pairs.get(0));

            player.HandScore = getHandValue(finalCards, PokerHand.FULL_HOUSE);
            player.Hand = PokerHand.FULL_HOUSE;
            return;
        }

        // Flush
        if (sameTypeCards.stream().anyMatch(l -> l.size() >= 5)) {
            List<Card> cards = sameTypeCards.stream().filter(l -> l.size() >= 5).findFirst().orElse(new ArrayList<>());
            cards.sort(Comparator.comparing(c -> c.Rank.ordinal()));

            if (cards.size() > 5) {
                cards.subList(0, (cards.size() - 5)).clear();
            }

            player.HandScore = getHandValue(cards, PokerHand.FLUSH);
            player.Hand = PokerHand.FLUSH;
            return;
        }

        // Straight
        if (allCards.size() >= 5) {
            List<Card> cards = new ArrayList<>(allCards);

            finalCards = getHighestStraight(cards);

            if (finalCards != null) {
                player.HandScore = getHandValue(cards, PokerHand.STRAIGHT);
                player.Hand = PokerHand.STRAIGHT;
                return;
            }
        }

        // Three of a kind
        if (sameRankCards.stream().anyMatch(l -> l.size() == 3)) {
            List<List<Card>> threeOfAKind = sameRankCards.stream().filter(l -> l.size() == 3).sorted(Comparator.comparing(l -> l.get(0).Rank.ordinal())).collect(Collectors.toList());

            finalCards = threeOfAKind.get(0);

            player.HandScore = getHandValue(finalCards, PokerHand.THREE_OF_A_KIND);
            player.Hand = PokerHand.THREE_OF_A_KIND;
            return;
        }

        // Two Pairs
        if (sameRankCards.stream().filter(l -> l.size() == 2).count() >= 2) {
            List<List<Card>> pairs = sameRankCards.stream().filter(l -> l.size() == 2).sorted(Comparator.comparing(l -> l.get(0).Rank.ordinal())).collect(Collectors.toList());

            finalCards = pairs.get(0);
            finalCards.addAll(pairs.get(1));

            player.HandScore = getHandValue(finalCards, PokerHand.TWO_PAIR);
            player.Hand = PokerHand.TWO_PAIR;
            return;
        }

        // Pair
        if (sameRankCards.stream().anyMatch(l -> l.size() == 2)) {
            List<List<Card>> pairs = sameRankCards.stream().filter(l -> l.size() == 2).sorted(Comparator.comparing(l -> l.get(0).Rank.ordinal())).collect(Collectors.toList());

            finalCards = pairs.get(0);

            player.HandScore = getHandValue(finalCards, PokerHand.PAIR);
            player.Hand = PokerHand.PAIR;
            return;
        }

        //High Card
        finalCards = new ArrayList<>();
        finalCards.add(allCards.get(0));

        player.HandScore = getHandValue(finalCards, PokerHand.HIGH_CARD);
        player.Hand = PokerHand.HIGH_CARD;
    }

    private List<List<Card>> getSameTypeCards(List<Card> cardList) {
        List<List<Card>> sameCards = new ArrayList<>();

        for (CardType type : CardType.values()) {
            sameCards.add(cardList.stream().filter(card -> card.Type.equals(type)).collect(Collectors.toList()));
        }
        sameCards.sort(Comparator.comparing(List::size));
        return sameCards;
    }
    private List<List<Card>> getSameRankCards(List<Card> cardList) {
        List<List<Card>> sameCards = new ArrayList<>();

        for (CardRank rank : CardRank.values()) {
            sameCards.add(cardList.stream().filter(card -> card.Rank.equals(rank)).collect(Collectors.toList()));
        }
        sameCards.sort(Comparator.comparing(List::size));
        return sameCards;
    }
    private List<Card> getHighestStraight(List<Card> cards) {
        cards.sort(Comparator.comparing(c -> c.Rank.ordinal()));

        List<Card> finalCards = new ArrayList<>();

        int inRow = 0;
        Card ACE = cards.stream().filter(c -> c.Rank.equals(CardRank.ACE)).findFirst().orElse(null);
        if (ACE != null) {
            inRow = 1;
            finalCards.add(ACE);
        }

        for (Card card : cards) {
            Card lastCard = finalCards.get(finalCards.size() - 1);

            if (inRow == 5 && card.Value.equals(lastCard.Value + 1)) {
                finalCards.remove(0);
                finalCards.add(card);
            }
            else if (inRow != 5 && ((card.Rank.equals(CardRank.TWO) && lastCard.Rank.equals(CardRank.ACE)) || card.Value.equals(lastCard.Value + 1))) {
                finalCards.add(card);
                inRow++;
            }
            else if (inRow != 5 && !card.Value.equals(lastCard.Value)) {
                finalCards.clear();
                finalCards.add(card);
                inRow = 1;
            }
        }

        if (inRow == 5) return finalCards;
        else return null;
    }
    private int getHandValue(List<Card> cards, PokerHand hand) {
        int value = 0;

        for (Card card : cards) value += card.Value;

        if (hand.equals(PokerHand.STRAIGHT) || hand.equals(PokerHand.STRAIGHT_FLUSH)) {
            if (cards.stream().anyMatch(c -> c.Rank.equals(CardRank.ACE)) && cards.stream().anyMatch(c -> c.Rank.equals(CardRank.TWO))) value -= 13;
        }
        return value;
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