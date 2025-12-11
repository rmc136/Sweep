package com.sweepgame.server.model;

import lombok.Data;
import java.util.List;

@Data
public class GameSession {

    private String sessionId;
    private List<PlayerConnection> players;
    private GameState state;

    public enum GameState {
        WAITING,
        IN_PROGRESS,
        FINISHED
    }

    // TODO: Add game logic integration
}
