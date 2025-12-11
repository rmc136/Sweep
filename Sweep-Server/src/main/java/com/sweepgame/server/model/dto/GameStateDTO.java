package com.sweepgame.server.model.dto;

import com.sweepgame.game.Card;
import lombok.Data;

import java.util.List;

@Data
public class GameStateDTO {
    
    private String sessionId;
    private String gameState;
    private int currentPlayerIndex;
    private List<Card> tableCards;
    private List<PlayerStateDTO> players;
    private String message;
    
    @Data
    public static class PlayerStateDTO {
        private String username;
        private int handSize;
        private int collectedSize;
        private int points;
        private int sweeps;
        private boolean isCurrentPlayer;
    }
}
