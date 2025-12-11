package com.sweepgame.network;

import com.sweepgame.game.Card;
import java.util.List;

public class GameStateDTO {
    private String sessionId;
    private String gameState;
    private int currentPlayerIndex;
    private List<Card> tableCards;
    private List<PlayerStateDTO> players;
    private String message;
    
    // Getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getGameState() { return gameState; }
    public void setGameState(String gameState) { this.gameState = gameState; }
    
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public void setCurrentPlayerIndex(int currentPlayerIndex) { this.currentPlayerIndex = currentPlayerIndex; }
    
    public List<Card> getTableCards() { return tableCards; }
    public void setTableCards(List<Card> tableCards) { this.tableCards = tableCards; }
    
    public List<PlayerStateDTO> getPlayers() { return players; }
    public void setPlayers(List<PlayerStateDTO> players) { this.players = players; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
