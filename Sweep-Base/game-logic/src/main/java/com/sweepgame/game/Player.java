package com.sweepgame.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class Player {
    private static final Logger logger = LoggerFactory.getLogger(Player.class);
    
    private final String name;
    private final List<Card> hand = new ArrayList<>();        // current hand (max 3 cards)
    private final List<Card> pointsStack = new ArrayList<>(); // collected cards for points
    private int brushes = 0;

    public Player(String name) {
        this.name = name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void drawCard(Deck deck) {
        if (deck == null) {
            logger.warn("{}: Cannot draw from null deck", name);
            return;
        }
        Card card = deck.draw();
        if (card != null) {
            hand.add(card);
            logger.debug("{}: Drew card {}", name, card);
        } else {
            logger.warn("{}: Tried to draw from empty deck", name);
        }
    }

    public List<Card> getPointsStack() {
        return pointsStack;
    }

    public void collectCards(List<Card> cards) {
        if (cards != null && !cards.isEmpty()) {
            pointsStack.addAll(cards);
            logger.debug("{}: Collected {} cards", name, cards.size());
        }
    }

    public void incrementBrushes() {
        brushes++;
        logger.debug("{}: Sweep! Total sweeps: {}", name, brushes);
    }

    public int getBrushes() {
        return brushes;
    }

    public int calculatePoints() {
        int points = 0;
        for (Card c : pointsStack) {
            if (c.getSuit() == Suit.DIAMONDS && c.getRank() == Rank.SEVEN){
                points += 2;// 7 of diamonds = 2 points
            }
            else if (c.getSuit() == Suit.DIAMONDS){
                points++;           // each diamond = 1 point
            }
            else if (c.getRank() == Rank.SEVEN){
                points++; // each 7 = 1 point
            }
        }
        
        logger.debug("{}: Calculated {} points from {} cards", name, points, pointsStack.size());
        return points;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }
}
