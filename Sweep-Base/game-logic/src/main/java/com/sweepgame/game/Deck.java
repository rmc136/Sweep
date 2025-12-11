package com.sweepgame.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.Collections;


public class Deck {
    private static final Logger logger = LoggerFactory.getLogger(Deck.class);
    
    private final List<Card> cards = new ArrayList<>();

    public Deck() {
        initializeDeck();
    }

    private void initializeDeck() {
        cards.clear();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                // Skip 8, 9, 10 cards (already omitted in Rank enum)
                cards.add(new Card(suit, rank));
            }
        }
        logger.debug("Deck initialized with {} cards", cards.size());
    }

    public void shuffle() {
        Collections.shuffle(cards);
        logger.debug("Deck shuffled");
    }

    public Card draw() {
        if (!cards.isEmpty()) {
            Card drawn = cards.remove(0);
            logger.debug("Card drawn: {}, {} cards remaining", drawn, cards.size());
            return drawn;
        }
        logger.warn("Attempted to draw from empty deck");
        return null; // deck empty
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    @Override
    public String toString() {
        return cards.toString();
    }

    public int size() {
        return cards.size();
    }

    public List<Card> getCards() {
        return cards;
    }
}
