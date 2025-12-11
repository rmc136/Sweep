package com.sweepgame.server.service;

import com.sweepgame.server.model.GameSession;
import com.sweepgame.server.model.PlayerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameSessionManager {
    private static final Logger logger = LoggerFactory.getLogger(GameSessionManager.class);

    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, String> playerToSessionMap = new ConcurrentHashMap<>();

    public GameSession createSession(boolean isRanked) {
        GameSession session = new GameSession(isRanked);
        activeSessions.put(session.getSessionId(), session);
        logger.info("Created new game session: {} (ranked: {})", session.getSessionId(), isRanked);
        return session;
    }

    public GameSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public GameSession getSessionByPlayer(String username) {
        String sessionId = playerToSessionMap.get(username);
        return sessionId != null ? activeSessions.get(sessionId) : null;
    }

    public void addPlayerToSession(String sessionId, PlayerConnection player) {
        GameSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        session.addPlayer(player);
        playerToSessionMap.put(player.getUsername(), sessionId);
        logger.info("Player {} joined session {}", player.getUsername(), sessionId);

        if (session.isFull()) {
            logger.info("Session {} is now full with {} players", sessionId, session.getPlayers().size());
        }
    }

    public void removePlayerFromSession(String username) {
        String sessionId = playerToSessionMap.remove(username);
        if (sessionId != null) {
            GameSession session = activeSessions.get(sessionId);
            if (session != null) {
                session.removePlayer(username);
                logger.info("Player {} left session {}", username, sessionId);

                if (session.getPlayers().isEmpty()) {
                    activeSessions.remove(sessionId);
                    logger.info("Session {} removed (no players)", sessionId);
                }
            }
        }
    }

    public void startSession(String sessionId) {
        GameSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        session.startGame();
        logger.info("Session {} started", sessionId);
    }

    public void finishSession(String sessionId) {
        GameSession session = activeSessions.get(sessionId);
        if (session == null) {
            return;
        }

        session.finishGame();
        logger.info("Session {} finished", sessionId);

        // Clean up player mappings
        for (PlayerConnection player : session.getPlayers()) {
            playerToSessionMap.remove(player.getUsername());
        }

        // Remove session after a delay (keep for history)
        // TODO: Save to database before removing
        activeSessions.remove(sessionId);
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public int getActivePlayers() {
        return playerToSessionMap.size();
    }
}
