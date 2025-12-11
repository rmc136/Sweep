package com.sweepgame.network;

public class PlayerStateDTO {
    private String username;
    private int handSize;
    private int collectedSize;
    private int points;
    private int sweeps;
    private boolean currentPlayer;
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public int getHandSize() { return handSize; }
    public void setHandSize(int handSize) { this.handSize = handSize; }
    
    public int getCollectedSize() { return collectedSize; }
    public void setCollectedSize(int collectedSize) { this.collectedSize = collectedSize; }
    
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    
    public int getSweeps() { return sweeps; }
    public void setSweeps(int sweeps) { this.sweeps = sweeps; }
    
    public boolean isCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(boolean currentPlayer) { this.currentPlayer = currentPlayer; }
}
