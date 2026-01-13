package com.sweepgame.server.controller;

import com.sweepgame.game.Card;
import com.sweepgame.game.Player;
import com.sweepgame.server.config.JwtConfig;
import com.sweepgame.server.model.GameSession;
import com.sweepgame.server.model.PlayerConnection;
import com.sweepgame.server.model.dto.GameStateDTO;
import com.sweepgame.server.model.dto.MoveDTO;
import com.sweepgame.server.service.GameSessionManager;
import com.sweepgame.server.service.MatchmakingService;
import com.sweepgame.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class GameController {
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    @Autowired
    private GameSessionManager sessionManager;

    @Autowired
    private MatchmakingService matchmakingService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/join")
    public void joinMatchmaking(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            logger.error("[MATCHMAKING] Join request received without principal");
            return;
        }
        String username = principal.getName();
        boolean isRanked = (boolean) payload.getOrDefault("ranked", false);

        logger.info("[MATCHMAKING] Player {} attempting to join {} queue", username, isRanked ? "ranked" : "casual");

        // Get user ID
        var user = userService.getUserByUsername(username);
        if (user == null) {
            logger.error("[MATCHMAKING] User {} not found in database", username);
            sendError(username, "User not found");
            return;
        }

        // Create player connection
        PlayerConnection player = new PlayerConnection(username, user.getId(), principal.getName());
        logger.debug("[MATCHMAKING] Created PlayerConnection for {}", username);

        // Join queue
        String sessionId = matchmakingService.joinQueue(player, isRanked);

        if (sessionId != null) {
            logger.info("[MATCHMAKING] Match FOUND for {}! Session ID: {}", username, sessionId);
            // Match found! Start game
            GameSession session = sessionManager.getSession(sessionId);
            sessionManager.startSession(sessionId);

            // Notify all players
            broadcastGameState(session);

            logger.info("[GAME] Session {} started and broadcast to players", sessionId);
        } else {
            // Still waiting for players
            int queueSize = matchmakingService.getQueueSize(isRanked);
            logger.info("[MATCHMAKING] Player {} added to queue. Current queue size: {}", username, queueSize);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "waiting");
            response.put("queueSize", queueSize);
            messagingTemplate.convertAndSendToUser(username, "/queue/matchmaking", response);

            // Broadcast new size to others in queue
            broadcastQueueUpdate(isRanked);
        }
    }

    @MessageMapping("/game/leave")
    public void leaveMatchmaking(Principal principal) {
        if (principal == null) {
            logger.warn("Received leaveMatchmaking request with null principal");
            return;
        }
        String username = principal.getName();
        logger.info("[MATCHMAKING] Player {} requesting to leave matchmaking queue", username);

        // We need to know which queue they were in to broadcast update.
        // For simplicity, we can just broadcast to both or check.
        // Given existing API doesn't return queue type, we'll brute force broadcast to
        // both for now
        // OR better: check if they were in a queue before removing?
        // MatchmakingService.leaveQueue handles removal efficiently.
        // Let's just broadcast to both queues to be safe and simple since we don't
        // track isRanked here easily.

        matchmakingService.leaveQueue(username);
        // Note: We don't remove from session here necessarily, as they might be just
        // leaving the queue, not an active game.
        // But if consistent with logic:
        sessionManager.removePlayerFromSession(username);
        logger.info("[MATCHMAKING] Player {} removed from queue/session", username);

        broadcastQueueUpdate(false); // Update casual
        broadcastQueueUpdate(true); // Update ranked
    }

    private void broadcastQueueUpdate(boolean isRanked) {
        int size = matchmakingService.getQueueSize(isRanked);
        List<PlayerConnection> waitingPlayers = matchmakingService.getQueuePlayers(isRanked);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "waiting");
        response.put("queueSize", size);

        for (PlayerConnection player : waitingPlayers) {
            messagingTemplate.convertAndSendToUser(
                    player.getUsername(),
                    "/queue/matchmaking",
                    response);
        }
    }

    @MessageMapping("/game/move")
    public void playCard(@Payload MoveDTO move, Principal principal) {
        String username = principal.getName();
        logger.info("Player {} playing card", username);

        GameSession session = sessionManager.getSessionByPlayer(username);
        if (session == null) {
            sendError(username, "Not in a game session");
            return;
        }

        if (!session.isActive()) {
            sendError(username, "Game is not active");
            return;
        }

        try {
            // Get player index
            int playerIndex = session.getPlayerIndex(username);
            if (playerIndex == -1) {
                sendError(username, "Player not found in session");
                return;
            }

            // Get the actual game Player object
            Player gamePlayer = session.getGamePlayer(playerIndex);
            if (gamePlayer == null) {
                sendError(username, "Game player not found");
                return;
            }

            // Validate it's player's turn
            if (!session.getGameLogic().getCurrentPlayer().equals(gamePlayer)) {
                sendError(username, "Not your turn");
                return;
            }

            // Get the card from hand
            Card handCard = gamePlayer.getHand().get(move.getHandCardIndex());

            // Get selected table cards
            List<Card> selectedCards = new ArrayList<>();
            for (int tableIndex : move.getTableCardIndices()) {
                selectedCards.add(session.getGameLogic().getTableCards().get(tableIndex));
            }

            // Play the card
            session.getGameLogic().playCardWithSelection(gamePlayer, handCard, selectedCards);

            // Broadcast updated game state
            broadcastGameState(session);

            // Check if need to deal new round
            if (session.getGameLogic().allHandsEmpty() && !session.getGameLogic().getDeck().isEmpty()) {
                session.getGameLogic().dealNewRound();
                broadcastGameState(session);
            }

            // Check if game is over
            if (session.getGameLogic().isGameOver()) {
                handleGameEnd(session);
            }

        } catch (Exception e) {
            logger.error("Error processing move", e);
            sendError(username, "Error processing move: " + e.getMessage());
        }
    }

    @MessageMapping("/game/ready")
    public void playerReady(Principal principal) {
        String username = principal.getName();
        logger.info("Player {} is ready", username);

        GameSession session = sessionManager.getSessionByPlayer(username);
        if (session == null) {
            sendError(username, "Not in a game session");
            return;
        }

        PlayerConnection player = session.getPlayerByUsername(username);
        if (player != null) {
            player.setReady(true);
        }

        // Check if all players are ready
        boolean allReady = session.getPlayers().stream().allMatch(PlayerConnection::isReady);
        if (allReady && session.getState() == GameSession.GameState.READY) {
            sessionManager.startSession(session.getSessionId());
            broadcastGameState(session);
        }
    }

    private void broadcastGameState(GameSession session) {
        GameStateDTO state = buildGameState(session);

        // Send to all players in the session
        for (PlayerConnection player : session.getPlayers()) {
            messagingTemplate.convertAndSendToUser(
                    player.getUsername(),
                    "/queue/game-state",
                    state);
        }
    }

    private GameStateDTO buildGameState(GameSession session) {
        GameStateDTO dto = new GameStateDTO();
        dto.setSessionId(session.getSessionId());
        dto.setGameState(session.getState().name());

        if (session.getGameLogic() != null) {
            Player currentPlayer = session.getGameLogic().getCurrentPlayer();
            int currentIndex = session.getGameLogic().getPlayers().indexOf(currentPlayer);
            dto.setCurrentPlayerIndex(currentIndex);
            dto.setTableCards(session.getGameLogic().getTableCards());

            List<GameStateDTO.PlayerStateDTO> playerStates = new ArrayList<>();
            for (int i = 0; i < session.getPlayers().size(); i++) {
                PlayerConnection pc = session.getPlayers().get(i);
                Player player = session.getGamePlayer(i);

                if (player != null) {
                    GameStateDTO.PlayerStateDTO ps = new GameStateDTO.PlayerStateDTO();
                    ps.setUsername(pc.getUsername());
                    ps.setHandSize(player.getHand().size());
                    ps.setCollectedSize(player.getPointsStack().size());
                    ps.setPoints(player.calculatePoints());
                    ps.setSweeps(player.getBrushes());
                    ps.setCurrentPlayer(i == currentIndex);

                    playerStates.add(ps);
                }
            }
            dto.setPlayers(playerStates);
        }

        return dto;
    }

    private void handleGameEnd(GameSession session) {
        logger.info("Game ended for session: {}", session.getSessionId());

        // Determine winner
        Player winner = session.getGameLogic().getWinner();
        int winnerIndex = session.getGameLogic().getPlayers().indexOf(winner);
        PlayerConnection winnerConnection = session.getPlayers().get(winnerIndex);

        // Broadcast final state
        GameStateDTO finalState = buildGameState(session);
        finalState.setMessage("Game Over! Winner: " + winnerConnection.getUsername());

        for (PlayerConnection player : session.getPlayers()) {
            messagingTemplate.convertAndSendToUser(
                    player.getUsername(),
                    "/queue/game-state",
                    finalState);
        }

        // TODO: Save game history to database
        // TODO: Update player stats

        // Finish session
        sessionManager.finishSession(session.getSessionId());
    }

    private void sendError(String username, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        messagingTemplate.convertAndSendToUser(username, "/queue/errors", error);
    }
}
