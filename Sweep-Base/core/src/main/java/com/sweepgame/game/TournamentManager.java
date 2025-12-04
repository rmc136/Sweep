package com.sweepgame.game;

import com.sweepgame.cards.Player;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentManager {
    
    private static TournamentManager instance;
    
    private String tournamentMode; 
    private final Map<String, Integer> playerWins;
    private int winsNeeded;
    private int gamesPlayed;
    
    private TournamentManager() {
        this.playerWins = new HashMap<>();
        this.winsNeeded = 1;
        this.gamesPlayed = 0;
    }
    
    public static TournamentManager getInstance() {
        if (instance == null) {
            instance = new TournamentManager();
        }
        return instance;
    }
    
    public void initializeTournament(String tournamentMode) {
        this.tournamentMode = tournamentMode;
        this.playerWins.clear();
        this.gamesPlayed = 0;
        
        switch (tournamentMode) {
            case "first_to_4":
                winsNeeded = 4;
                break;
            case "first_to_8":
                winsNeeded = 8;
                break;
            case "single":
            default:
                winsNeeded = 1;
                break;
        }
    }
    
    /**
     * Reset tournament state without setting a mode.
     * Used when returning to home screen.
     */
    public void reset() {
        this.tournamentMode = null;
        this.playerWins.clear();
        this.gamesPlayed = 0;
        this.winsNeeded = 1;
    }
    
    public void initializePlayers(List<Player> players) {
        if (playerWins.isEmpty()) {
            for (Player p : players) {
                playerWins.put(p.getName(), 0);
            }
        }
    }
    
    public void recordWin(String playerName) {
        playerWins.put(playerName, playerWins.getOrDefault(playerName, 0) + 1);
        gamesPlayed++;
    }
    
    public int getWins(String playerName) {
        return playerWins.getOrDefault(playerName, 0);
    }
    
    public boolean isTournamentComplete() {
        for (int wins : playerWins.values()) {
            if (wins >= winsNeeded) {
                return true;
            }
        }
        return false;
    }
    
    public String getTournamentWinner() {
        if (!isTournamentComplete()) {
            return null;
        }
        
        String winner = null;
        int maxWins = 0;
        for (Map.Entry<String, Integer> entry : playerWins.entrySet()) {
            if (entry.getValue() > maxWins) {
                maxWins = entry.getValue();
                winner = entry.getKey();
            }
        }
        return winner;
    }
    
    public String getTournamentMode() {
        return tournamentMode;
    }
    
    public int getWinsNeeded() {
        return winsNeeded;
    }
    
    public boolean isSingleGame() {
        return "single".equals(tournamentMode);
    }
    
    /**
     * Get the starting player index for the current game (rotates anti-clockwise)
     * Player 0 starts game 1, Player 2 starts game 2, Player 1 starts game 3, etc.
     */
    public int getStartingPlayerIndex() {
        // Anti-clockwise rotation: 0 -> 2 -> 1 -> 0
        // This is equivalent to: (3 - gamesPlayed) % 3
        return (3 - (gamesPlayed % 3)) % 3;
    }
}
