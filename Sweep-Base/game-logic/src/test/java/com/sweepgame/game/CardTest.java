package com.sweepgame.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void getValue_returnsRankValue() {
        Card card = new Card(Suit.HEARTS, Rank.KING);
        assertEquals(10, card.getValue());
    }

    @Test
    void getValue_aceIsOne() {
        Card ace = new Card(Suit.CLUBS, Rank.ACE);
        assertEquals(1, ace.getValue());
    }

    @Test
    void getValue_queenIsEight() {
        // Rank.QUEEN has value 8 (not 12 as in standard decks)
        Card queen = new Card(Suit.HEARTS, Rank.QUEEN);
        assertEquals(8, queen.getValue());
    }

    @Test
    void getValue_jackIsNine() {
        // Rank.JACK has value 9
        Card jack = new Card(Suit.SPADES, Rank.JACK);
        assertEquals(9, jack.getValue());
    }

    @Test
    void getSuit_returnsSuit() {
        Card card = new Card(Suit.DIAMONDS, Rank.SEVEN);
        assertEquals(Suit.DIAMONDS, card.getSuit());
    }

    @Test
    void getRank_returnsRank() {
        Card card = new Card(Suit.CLUBS, Rank.FIVE);
        assertEquals(Rank.FIVE, card.getRank());
    }

    @Test
    void toString_containsRankAndSuit() {
        Card card = new Card(Suit.HEARTS, Rank.ACE);
        String result = card.toString();
        assertTrue(result.contains("ACE") || result.contains("Ace") || result.contains("ace"),
                "toString should mention the rank");
        assertTrue(result.contains("HEARTS") || result.contains("Hearts") || result.contains("hearts"),
                "toString should mention the suit");
    }

    @Test
    void getImageName_ace() {
        Card card = new Card(Suit.HEARTS, Rank.ACE);
        assertEquals("ace_of_hearts.png", card.getImageName());
    }

    @Test
    void getImageName_jack() {
        Card card = new Card(Suit.SPADES, Rank.JACK);
        assertEquals("jack_of_spades.png", card.getImageName());
    }

    @Test
    void getImageName_queen() {
        Card card = new Card(Suit.DIAMONDS, Rank.QUEEN);
        assertEquals("queen_of_diamonds.png", card.getImageName());
    }

    @Test
    void getImageName_king() {
        Card card = new Card(Suit.CLUBS, Rank.KING);
        assertEquals("king_of_clubs.png", card.getImageName());
    }

    @Test
    void getImageName_numberedCard_usesNumericValue() {
        // Rank.SEVEN has getValue() == 7
        Card card = new Card(Suit.HEARTS, Rank.SEVEN);
        assertEquals("7_of_hearts.png", card.getImageName());
    }

    @Test
    void getImageName_two() {
        Card card = new Card(Suit.CLUBS, Rank.TWO);
        assertEquals("2_of_clubs.png", card.getImageName());
    }

    @Test
    void defaultConstructor_doesNotThrow() {
        assertDoesNotThrow(() -> new Card());
    }
}
