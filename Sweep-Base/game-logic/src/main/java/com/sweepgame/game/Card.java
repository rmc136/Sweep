package com.sweepgame.game;

public class Card {
    private Suit suit;
    private Rank rank;

    public Card() {
        // Default constructor for serialization
    }

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    public int getValue() {
        return rank.getValue();
    }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }

    public String getImageName() {
        String rankName;

        switch (rank) {
            case ACE:
                rankName = "ace";
                break;
            case JACK:
                rankName = "jack";
                break;
            case QUEEN:
                rankName = "queen";
                break;
            case KING:
                rankName = "king";
                break;
            default:
                rankName = String.valueOf(rank.getValue()); // number cards 2-7
                break;
        }

        return rankName + "_of_" + suit.toString().toLowerCase() + ".png";
    }
}
