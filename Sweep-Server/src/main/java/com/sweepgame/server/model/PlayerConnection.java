package com.sweepgame.server.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlayerConnection {
    
    private String username;
    private Long userId;
    private String sessionId;
    private String webSocketSessionId;
    private LocalDateTime connectedAt;
    private boolean ready;
    
    public PlayerConnection(String username, Long userId, String webSocketSessionId) {
        this.username = username;
        this.userId = userId;
        this.webSocketSessionId = webSocketSessionId;
        this.connectedAt = LocalDateTime.now();
        this.ready = false;
    }
}
