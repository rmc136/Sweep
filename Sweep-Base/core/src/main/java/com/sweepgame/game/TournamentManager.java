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
    
    private TournamentManager() {
        this.playerWins = new HashMap<>();
        this.winsNeeded = 1;
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
    
    public void initializePlayers(List<Player> players) {
        if (playerWins.isEmpty()) {
            for (Player p : players) {
                playerWins.put(p.getName(), 0);
            }
        }
    }
    
    public void recordWin(String playerName) {
        playerWins.put(playerName, playerWins.getOrDefault(playerName, 0) + 1);
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
}
