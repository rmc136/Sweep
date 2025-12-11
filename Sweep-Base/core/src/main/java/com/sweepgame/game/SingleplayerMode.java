package com.sweepgame.game;

import com.sweepgame.game.Card;
import com.sweepgame.game.Player;
import com.sweepgame.game.SweepLogic;
import java.util.List;

/**
 * Singleplayer game mode using local SweepLogic
 */
public class SingleplayerMode implements GameMode {
    
    private final SweepLogic logic;
    
    public SingleplayerMode() {
        this.logic = new SweepLogic();
    }
    
    @Override
    public void startGame(int startingPlayerIndex) {
        logic.startGame(startingPlayerIndex);
    }
    
    @Override
    public List<Player> getPlayers() {
        return logic.getPlayers();
    }
    
    @Override
    public List<Card> getTableCards() {
        return logic.getTableCards();
    }
    
    @Override
    public Player getCurrentPlayer() {
        return logic.getCurrentPlayer();
    }
    
    @Override
    public void playCard(Player player, Card handCard, List<Card> selectedTableCards) {
        logic.playCardWithSelection(player, handCard, selectedTableCards);
    }
    
    @Override
    public boolean allHandsEmpty() {
        return logic.allHandsEmpty();
    }
    
    @Override
    public void dealNewRound() {
        logic.dealNewRound();
    }
    
    @Override
    public boolean isGameOver() {
        return logic.isGameOver();
    }
    
    @Override
    public Player getWinner() {
        return logic.getWinner();
    }
    
    @Override
    public void finishGame() {
        logic.finishGame();
    }
    
    @Override
    public List<Card> getLastCollectedCards() {
        return logic.getLastCollectedCards();
    }
    
    @Override
    public boolean isMultiplayer() {
        return false;
    }
    
    @Override
    public boolean isMyTurn() {
        return true; // In singleplayer, it's always the player's turn
    }
    
    public SweepLogic getLogic() {
        return logic;
    }
}
