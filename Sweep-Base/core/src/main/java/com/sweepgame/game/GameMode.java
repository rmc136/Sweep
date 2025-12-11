package com.sweepgame.game;

import com.sweepgame.game.Card;
import com.sweepgame.game.Player;
import java.util.List;

/**
 * Interface that abstracts game logic for both singleplayer and multiplayer modes
 */
public interface GameMode {
    
    /**
     * Start the game
     */
    void startGame(int startingPlayerIndex);
    
    /**
     * Get all players in the game
     */
    List<Player> getPlayers();
    
    /**
     * Get cards currently on the table
     */
    List<Card> getTableCards();
    
    /**
     * Get the current player whose turn it is
     */
    Player getCurrentPlayer();
    
    /**
     * Play a card with selected table cards
     */
    void playCard(Player player, Card handCard, List<Card> selectedTableCards);
    
    /**
     * Check if all players' hands are empty
     */
    boolean allHandsEmpty();
    
    /**
     * Deal a new round of cards
     */
    void dealNewRound();
    
    /**
     * Check if the game is over
     */
    boolean isGameOver();
    
    /**
     * Get the winner of the game
     */
    Player getWinner();
    
    /**
     * Finish the game
     */
    void finishGame();
    
    /**
     * Get the last collected cards
     */
    List<Card> getLastCollectedCards();
    
    /**
     * Check if this is a multiplayer game
     */
    boolean isMultiplayer();
    
    /**
     * Check if it's the local player's turn (always true for singleplayer)
     */
    boolean isMyTurn();
}
