package com.sweepgame.game;

import com.sweepgame.cards.Card;
import com.sweepgame.cards.Deck;
import com.sweepgame.cards.Player;
import java.util.*;

public class SweepLogic {

    private final Deck deck = new Deck();
    private final List<Player> players = new ArrayList<>();
    private final List<Card> tableCards = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private List<Card> lastCollected = new ArrayList<>();// who plays now
    private boolean isFirstRound = true;

    public void startGame() {
        players.clear();
        tableCards.clear();
        currentPlayerIndex = 0;

        players.add(new Player("Johnny"));
        players.add(new Player("Joni"));
        players.add(new Player("Rodrigo"));

        deck.shuffle();

        for (Player p : players) {
            for (int i = 0; i < 3; i++) p.drawCard(deck);
        }

        for (int i = 0; i < 4; i++) tableCards.add(deck.draw());
             
        if (isFirstRound) {
            int tableSum = 0;
            for (Card c : tableCards) {
                tableSum += c.getValue();
            }
            
            if (tableSum == 15) {
                Player firstPlayer = players.get(0);
                firstPlayer.collectCards(new ArrayList<>(tableCards));
                firstPlayer.incrementBrushes();
                tableCards.clear();
                isFirstRound = false;
            }
        }
    }

    public boolean isGameOver() {
        for (Player p : players) if (!p.getHand().isEmpty()) return false;
        return deck.isEmpty();
    }

    public List<Player> getPlayers() { return players; }
    public List<Card> getTableCards() { return tableCards; }

    public void playCard(Player player, Card card, List<Card> selected) {
        if (!player.getHand().contains(card)) return;

        List<Card> collected = checkSum15(card, false); // Normal sum-15 logic
        player.getHand().remove(card);

        if (selected.isEmpty()){
            tableCards.add(card);
        }
        else if (!collected.isEmpty()) {
            player.collectCards(collected);
            if (collected.size() == tableCards.size() + 1) player.incrementBrushes();
            tableCards.removeAll(collected);
        } else {
            tableCards.add(card);
        }
        
        lastCollected = new ArrayList<>(collected);
        advanceTurn();
    }

    public void playCardWithSelection(Player player, Card handCard, List<Card> selected) {
        if (!player.getHand().contains(handCard)) return;

        int sum = handCard.getValue();
        for (Card c : selected) sum += c.getValue();

        player.getHand().remove(handCard);

        List<Card> collected = new ArrayList<>();

        if (sum == 15 && new HashSet<>(tableCards).containsAll(selected)) {
            System.out.println("Cards i take from table and my card: " + selected + handCard);
            player.collectCards(selected);
            player.collectCards(Collections.singletonList(handCard));
            if (selected.size() == tableCards.size()) player.incrementBrushes();
            tableCards.removeAll(selected);

            collected.addAll(selected);
            collected.add(handCard);
        } else {
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
        for (Player p : players) {
            for (int i = 0; i < 3; i++) {
                if (!deck.isEmpty()) {
                    p.drawCard(deck);
                }
            }
        }
    }

    public Player getWinner() {
        if (!isGameOver()) return null;

        Player winner = null;
        int bestScore = Integer.MIN_VALUE;

        for (Player p : players) {
            int score = p.calculatePoints() + p.getBrushes(); 
            if (winner == null || score > bestScore) {
                bestScore = score;
                winner = p;
            } else if (score == bestScore) {
                winner = tiebreak(winner, p);
            }
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
            lastPlayer.collectCards(new ArrayList<>(tableCards));
            tableCards.clear();
        }
    }

}
