package com.sweepgame.game;

import com.sweepgame.game.Card;
import com.sweepgame.game.Deck;
import com.sweepgame.game.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class SweepLogic {
    private static final Logger logger = LoggerFactory.getLogger(SweepLogic.class);

    private final Deck deck = new Deck();
    private final List<Player> players = new ArrayList<>();
    private final List<Card> tableCards = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private List<Card> lastCollected = new ArrayList<>();
    private boolean isFirstRound = true;

    public void startGame() {
        startGame(0);
    }
    
    public void startGame(int startingPlayerIndex) {
        logger.info("Starting game with starting player index: {}", startingPlayerIndex);
        
        players.clear();
        tableCards.clear();
        currentPlayerIndex = startingPlayerIndex % 3;

        players.add(new Player("Johnny"));
        players.add(new Player("Joni"));
        players.add(new Player("Rodrigo"));

        deck.shuffle();
        logger.debug("Deck shuffled, {} cards total", deck.getCards().size());

        for (Player p : players) {
            for (int i = 0; i < 3; i++) p.drawCard(deck);
        }
        logger.debug("Dealt 3 cards to each player");
        
        for (int i = 0; i < 4; i++) tableCards.add(deck.draw());
        logger.debug("Dealt 4 cards to table: {}", tableCards);
             
        if (isFirstRound) {
            int tableSum = 0;
            for (Card c : tableCards) {
                tableSum += c.getValue();
            }
            
            if (tableSum == 15) {
                Player firstPlayer = players.get(startingPlayerIndex);
                logger.info("Initial table sum is 15! Awarding sweep to {}", firstPlayer.getName());
                firstPlayer.collectCards(new ArrayList<>(tableCards));
                firstPlayer.incrementBrushes();
                tableCards.clear();
                isFirstRound = false;
            }
        }
        
        logger.info("Game started successfully");
    }

    public boolean isGameOver() {
        for (Player p : players) if (!p.getHand().isEmpty()) return false;
        return deck.isEmpty();
    }

    public List<Player> getPlayers() { return players; }
    public List<Card> getTableCards() { return tableCards; }

    public void playCard(Player player, Card card, List<Card> selected) {
        if (player == null || card == null) {
            logger.warn("Invalid playCard call: player={}, card={}", player, card);
            return;
        }
        
        if (!player.getHand().contains(card)) {
            logger.warn("Player {} tried to play card {} not in hand", player.getName(), card);
            return;
        }

        List<Card> collected = checkSum15(card, false);
        player.getHand().remove(card);

        if (selected.isEmpty()){
            tableCards.add(card);
            logger.debug("Player {} played {} to table (no capture)", player.getName(), card);
        }
        else if (!collected.isEmpty()) {
            player.collectCards(collected);
            boolean isSweep = collected.size() == tableCards.size() + 1;
            if (isSweep) {
                player.incrementBrushes();
                logger.info("SWEEP! Player {} cleared the table with {}", player.getName(), card);
            } else {
                logger.debug("Player {} played {} and collected {} cards", player.getName(), card, collected.size());
            }
            tableCards.removeAll(collected);
        } else {
            tableCards.add(card);
            logger.debug("Player {} played {} to table (no valid capture)", player.getName(), card);
        }
        
        lastCollected = new ArrayList<>(collected);
        advanceTurn();
    }

    public void playCardWithSelection(Player player, Card handCard, List<Card> selected) {
        if (player == null || handCard == null) {
            logger.warn("Invalid playCardWithSelection call: player={}, card={}", player, handCard);
            return;
        }
        
        if (!player.getHand().contains(handCard)) {
            logger.warn("Player {} tried to play card {} not in hand", player.getName(), handCard);
            return;
        }

        int sum = handCard.getValue();
        for (Card c : selected) sum += c.getValue();

        player.getHand().remove(handCard);

        List<Card> collected = new ArrayList<>();

        if (sum == 15 && new HashSet<>(tableCards).containsAll(selected)) {
            logger.debug("Player {} captured: table cards {} + hand card {} = 15", 
                        player.getName(), selected, handCard);
            player.collectCards(selected);
            player.collectCards(Collections.singletonList(handCard));
            
            boolean isSweep = selected.size() == tableCards.size();
            if (isSweep) {
                player.incrementBrushes();
                logger.info("SWEEP! Player {} cleared the table", player.getName());
            }
            tableCards.removeAll(selected);

            collected.addAll(selected);
            collected.add(handCard);
        } else {
            if (sum != 15) {
                logger.debug("Player {} played {} to table (sum={}, expected 15)", 
                            player.getName(), handCard, sum);
            } else {
                logger.warn("Player {} tried invalid selection: cards not on table", player.getName());
            }
            tableCards.add(handCard);
        }

        lastCollected = collected;

        advanceTurn();
    }


    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    private void advanceTurn() {
        currentPlayerIndex = (currentPlayerIndex - 1 + players.size()) % players.size();
    }

    private List<Card> checkSum15(Card played, boolean isFirstRound) {
        List<Card> collected = new ArrayList<>();
        int n = tableCards.size();
        for (int mask = 0; mask < (1 << n); mask++) {
            int sum = 0;
            if (!isFirstRound) {
                sum = played.getValue();   
            }
            List<Card> subset = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    sum += tableCards.get(i).getValue();
                    subset.add(tableCards.get(i));
                }
            }
            if (sum == 15) {
                collected.addAll(subset);
                collected.add(played);
                return collected;
            }
        }

        return collected;
    }

    public List<Card> findRandomValidSum15(Card played) {
        List<List<Card>> allValidCombinations = new ArrayList<>();
        int n = tableCards.size();
        
        for (int mask = 0; mask < (1 << n); mask++) {
            int sum = played.getValue();
            List<Card> subset = new ArrayList<>();
            
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    sum += tableCards.get(i).getValue();
                    subset.add(tableCards.get(i));
                }
            }
            
            if (sum == 15 && !subset.isEmpty()) {
                allValidCombinations.add(new ArrayList<>(subset));
            }
        }
        
        if (!allValidCombinations.isEmpty()) {
            int randomIndex = (int) (Math.random() * allValidCombinations.size());
            return allValidCombinations.get(randomIndex);
        }
        
        return new ArrayList<>();
    }

    public Deck getDeck() {
        return deck;
    }

    public boolean allHandsEmpty() {
        for (Player p : players) {
            if (!p.getHand().isEmpty()) return false;
        }
        return true;
    }

    public void dealNewRound() {
        logger.debug("Dealing new round, {} cards remaining in deck", deck.getCards().size());
        for (Player p : players) {
            for (int i = 0; i < 3; i++) {
                if (!deck.isEmpty()) {
                    p.drawCard(deck);
                } else {
                    logger.warn("Deck empty while dealing to {}", p.getName());
                }
            }
        }
        logger.debug("New round dealt, {} cards remaining", deck.getCards().size());
    }

    public Player getWinner() {
        if (!isGameOver()) return null;

        Player winner = null;
        int bestScore = Integer.MIN_VALUE;

        for (Player p : players) {
            int score = p.calculatePoints() + p.getBrushes();
            logger.debug("Player {} final score: {} points + {} sweeps = {}", 
                        p.getName(), p.calculatePoints(), p.getBrushes(), score);
            
            if (winner == null || score > bestScore) {
                bestScore = score;
                winner = p;
            } else if (score == bestScore) {
                logger.debug("Tie between {} and {}, using tiebreak", winner.getName(), p.getName());
                winner = tiebreak(winner, p);
            }
        }
        
        if (winner != null) {
            logger.info("Game winner: {} with {} points", winner.getName(), bestScore);
        }
        return winner;
    }

    
    private Player tiebreak(Player a, Player b) {
        int aCards = a.getPointsStack().size();
        int bCards = b.getPointsStack().size();

        if (aCards > bCards) return a;
        if (bCards > aCards) return b;

        return Math.random() < 0.5 ? a : b;
    }

    public List<Card> getLastCollectedCards() {
        return lastCollected;
    }

    public void finishGame() {
        if (!tableCards.isEmpty()) {
            Player lastPlayer = players.get((currentPlayerIndex - 1 + players.size()) % players.size());
            logger.debug("Game finished, {} remaining table cards awarded to {}", 
                        tableCards.size(), lastPlayer.getName());
            lastPlayer.collectCards(new ArrayList<>(tableCards));
            tableCards.clear();
        }
        logger.info("Game finished");
    }

}
