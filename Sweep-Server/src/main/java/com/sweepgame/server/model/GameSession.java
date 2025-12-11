package com.sweepgame.server.model;

import com.sweepgame.game.Player;
import com.sweepgame.game.SweepLogic;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class GameSession {
    
    private String sessionId;
    private List<PlayerConnection> players;
    private SweepLogic gameLogic;
    private GameState state;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private boolean isRanked;
    
    public enum GameState {
        WAITING,     // Waiting for players
        READY,       // All players connected
        IN_PROGRESS, // Game is running
        FINISHED     // Game completed
    }
    
    public GameSession(boolean isRanked) {
        this.sessionId = UUID.randomUUID().toString();
        this.players = new ArrayList<>();
        this.state = GameState.WAITING;
        this.createdAt = LocalDateTime.now();
        this.isRanked = isRanked;
    }
    
    public void addPlayer(PlayerConnection player) {
        if (players.size() >= 3) {
            throw new IllegalStateException("Game session is full");
        }
        players.add(player);
        player.setSessionId(this.sessionId);
        
        if (players.size() == 3) {
            state = GameState.READY;
        }
    }
    
    public void removePlayer(String username) {
        players.removeIf(p -> p.getUsername().equals(username));
        
        if (state == GameState.IN_PROGRESS) {
            state = GameState.FINISHED; // Game ends if someone leaves
        }
    }
    
    public void startGame() {
        if (state != GameState.READY) {
            throw new IllegalStateException("Cannot start game in state: " + state);
        }
        
        // Initialize game logic
        gameLogic = new SweepLogic();
        gameLogic.startGame();
        
        state = GameState.IN_PROGRESS;
        startedAt = LocalDateTime.now();
    }
    
    public void finishGame() {
        if (gameLogic != null) {
            gameLogic.finishGame();
        }
        state = GameState.FINISHED;
        finishedAt = LocalDateTime.now();
    }
    
    public boolean isFull() {
        return players.size() >= 3;
    }
    
    public boolean isActive() {
        return state == GameState.IN_PROGRESS;
    }
    
    public PlayerConnection getPlayerByUsername(String username) {
        return players.stream()
                .filter(p -> p.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }
    
    public int getPlayerIndex(String username) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUsername().equals(username)) {
                return i;
            }
        }
        return -1;
    }
    
    public Player getGamePlayer(int index) {
        if (gameLogic == null || index < 0 || index >= gameLogic.getPlayers().size()) {
            return null;
        }
        return gameLogic.getPlayers().get(index);
    }
}
