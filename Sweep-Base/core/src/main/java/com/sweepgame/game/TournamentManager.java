package com.sweepgame.game;

import com.sweepgame.game.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentManager {
    private static final Logger logger = LoggerFactory.getLogger(TournamentManager.class);
    
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
        if (tournamentMode == null || tournamentMode.isEmpty()) {
            logger.warn("Invalid tournament mode, defaulting to 'single'");
            tournamentMode = "single";
        }
        
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
        
        logger.info("Tournament initialized: mode={}, wins needed={}", tournamentMode, winsNeeded);
    }
    
    /**
     * Reset tournament state without setting a mode.
     * Used when returning to home screen.
     */
    public void reset() {
        logger.info("Tournament reset");
        this.tournamentMode = null;
        this.playerWins.clear();
        this.gamesPlayed = 0;
        this.winsNeeded = 1;
    }
    
    public void initializePlayers(List<Player> players) {
        if (players == null || players.isEmpty()) {
            logger.warn("Cannot initialize with null or empty player list");
            return;
        }
        
        if (playerWins.isEmpty()) {
            for (Player p : players) {
                playerWins.put(p.getName(), 0);
            }
            logger.debug("Initialized {} players for tournament", players.size());
        }
    }
    
    public void recordWin(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            logger.warn("Cannot record win for null/empty player name");
            return;
        }
        
        int newWins = playerWins.getOrDefault(playerName, 0) + 1;
        playerWins.put(playerName, newWins);
        gamesPlayed++;
        
        logger.info("Win recorded: {} now has {}/{} wins (game {} of tournament)", 
                   playerName, newWins, winsNeeded, gamesPlayed);
    }
    
    public int getWins(String playerName) {
        return playerWins.getOrDefault(playerName, 0);
    }
    
    public boolean isTournamentComplete() {
        for (Map.Entry<String, Integer> entry : playerWins.entrySet()) {
            if (entry.getValue() >= winsNeeded) {
                logger.info("Tournament complete! Winner: {} with {} wins", 
                           entry.getKey(), entry.getValue());
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
        int startingPlayer = (3 - (gamesPlayed % 3)) % 3;
        logger.debug("Game {}: Starting player index = {}", gamesPlayed + 1, startingPlayer);
        return startingPlayer;
    }
}
