package com.sweepgame.server.model;

import lombok.Data;

@Data
public class PlayerConnection {

    private Long userId;
    private String username;
    private String sessionId;
    private boolean connected;

    // TODO: Add WebSocket session tracking
}
