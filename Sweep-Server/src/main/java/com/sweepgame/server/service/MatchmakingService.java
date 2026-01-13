package com.sweepgame.server.service;

import com.sweepgame.server.model.GameSession;
import com.sweepgame.server.model.PlayerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MatchmakingService {
    private static final Logger logger = LoggerFactory.getLogger(MatchmakingService.class);

    @Autowired
    private GameSessionManager gameSessionManager;

    private final Queue<PlayerConnection> casualQueue = new ConcurrentLinkedQueue<>();
    private final Queue<PlayerConnection> rankedQueue = new ConcurrentLinkedQueue<>();

    public String joinQueue(PlayerConnection player, boolean isRanked) {
        Queue<PlayerConnection> queue = isRanked ? rankedQueue : casualQueue;

        logger.info("Player {} joining {} queue", player.getUsername(), isRanked ? "ranked" : "casual");
        queue.add(player);

        // Try to form a match
        return tryFormMatch(isRanked);
    }

    public void leaveQueue(String username) {
        casualQueue.removeIf(p -> p.getUsername().equals(username));
        rankedQueue.removeIf(p -> p.getUsername().equals(username));
        logger.info("Player {} left matchmaking queue", username);
    }

    private String tryFormMatch(boolean isRanked) {
        Queue<PlayerConnection> queue = isRanked ? rankedQueue : casualQueue;
        int currentSize = queue.size();
        logger.debug("[MATCHMAKING] Checking queue (Ranked: {}). Current size: {}", isRanked, currentSize);

        if (currentSize >= 3) {
            logger.info("[MATCHMAKING] Threshold reached (3 players). Forming match...");
            // Create new game session
            GameSession session = gameSessionManager.createSession(isRanked);

            // Add 3 players to the session
            for (int i = 0; i < 3; i++) {
                PlayerConnection player = queue.poll();
                if (player != null) {
                    logger.debug("[MATCHMAKING] Adding player {} to session {}", player.getUsername(),
                            session.getSessionId());
                    gameSessionManager.addPlayerToSession(session.getSessionId(), player);
                }
            }

            logger.info("[MATCHMAKING] Match successfully formed! Session: {} (ranked: {})", session.getSessionId(),
                    isRanked);
            return session.getSessionId();
        } else {
            logger.debug("[MATCHMAKING] Not enough players yet. Need 3, have {}", currentSize);
        }

        return null; // Not enough players yet
    }

    public int getQueueSize(boolean isRanked) {
        return isRanked ? rankedQueue.size() : casualQueue.size();
    }

    public int getTotalQueueSize() {
        return casualQueue.size() + rankedQueue.size();
    }

    public java.util.List<PlayerConnection> getQueuePlayers(boolean isRanked) {
        return new java.util.ArrayList<>(isRanked ? rankedQueue : casualQueue);
    }
}
