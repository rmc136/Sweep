package com.sweepgame.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.*;

public class WebSocketManager {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketManager.class);
    
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private final Gson gson = new Gson();
    private GameStateListener listener;
    private String serverUrl = "http://localhost:8080/ws";
    private String accessToken;
    private boolean connected = false;
    
    public interface GameStateListener {
        void onGameStateUpdate(GameStateDTO state);
        void onMatchFound(String sessionId);
        void onQueueUpdate(int queueSize);
        void onError(String message);
        void onDisconnect();
    }
    
    public void setListener(GameStateListener listener) {
        this.listener = listener;
    }
    
    public void connect(String token) {
        this.accessToken = token;
        
        try {
            // Create SockJS client with WebSocket transport
            List<Transport> transports = new ArrayList<>();
            transports.add(new WebSocketTransport(new StandardWebSocketClient()));
            SockJsClient sockJsClient = new SockJsClient(transports);
            
            // Create STOMP client
            stompClient = new WebSocketStompClient(sockJsClient);
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());
            
            // Create STOMP headers with JWT authentication
            StompHeaders connectHeaders = new StompHeaders();
            connectHeaders.add("Authorization", "Bearer " + token);
            
            logger.info("Connecting to STOMP server at {}", serverUrl);
            
            // Connect (returns ListenableFuture in Spring 5.3)
            stompClient.connect(serverUrl, (org.springframework.web.socket.WebSocketHttpHeaders) null, connectHeaders, new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    logger.info("STOMP connected successfully");
                    stompSession = session;
                    connected = true;
                    subscribeToTopics();
                }
                
                @Override
                public void handleException(StompSession session, StompCommand command, 
                                          StompHeaders headers, byte[] payload, Throwable exception) {
                    logger.error("STOMP exception - command: {}", command, exception);
                    if (listener != null) {
                        listener.onError("Connection error: " + exception.getMessage());
                    }
                }
                
                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    logger.error("Transport error", exception);
                    connected = false;
                    if (listener != null) {
                        listener.onDisconnect();
                    }
                }
                
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }
                
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    if (payload != null) {
                        handleMessage(payload.toString());
                    }
                }
            });
            
        } catch (Exception e) {
            logger.error("Failed to connect to STOMP server", e);
            if (listener != null) {
                listener.onError("Failed to connect: " + e.getMessage());
            }
        }
    }
    
    private void subscribeToTopics() {
        if (stompSession == null || !stompSession.isConnected()) {
            logger.warn("Cannot subscribe - session not connected");
            return;
        }
        
        try {
            // Subscribe to game state updates
            stompSession.subscribe("/user/queue/game-state", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }
                
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    logger.debug("Received game-state message");
                    handleMessage(payload.toString());
                }
            });
            
            // Subscribe to matchmaking updates
            stompSession.subscribe("/user/queue/matchmaking", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }
                
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    logger.debug("Received matchmaking message");
                    handleMessage(payload.toString());
                }
            });
            
            // Subscribe to error messages
            stompSession.subscribe("/user/queue/errors", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }
                
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    logger.debug("Received error message");
                    handleMessage(payload.toString());
                }
            });
            
            logger.info("Subscribed to all topics successfully");
            
        } catch (Exception e) {
            logger.error("Failed to subscribe to topics", e);
        }
    }
    
    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            logger.info("STOMP session disconnected");
        }
        if (stompClient != null) {
            stompClient.stop();
            logger.info("STOMP client stopped");
        }
        connected = false;
    }
    
    public boolean isConnected() {
        return connected && stompSession != null && stompSession.isConnected();
    }
    
    public void joinQueue(boolean isRanked) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ranked", isRanked);
        sendMessage("/app/game/join", payload);
    }
    
    public void leaveQueue() {
        sendMessage("/app/game/leave", new HashMap<>());
    }
    
    public void playCard(int handCardIndex, List<Integer> tableCardIndices) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("handCardIndex", handCardIndex);
        payload.put("tableCardIndices", tableCardIndices);
        sendMessage("/app/game/move", payload);
    }
    
    public void sendReady() {
        sendMessage("/app/game/ready", new HashMap<>());
    }
    
    private void sendMessage(String destination, Object payload) {
        if (!isConnected()) {
            logger.warn("Cannot send message to {}, not connected", destination);
            return;
        }
        
        try {
            stompSession.send(destination, payload);
            logger.debug("Sent message to {}: {}", destination, payload);
            
        } catch (Exception e) {
            logger.error("Failed to send message to {}", destination, e);
            if (listener != null) {
                listener.onError("Failed to send message: " + e.getMessage());
            }
        }
    }
    
    private void handleMessage(String message) {
        try {
            logger.debug("Handling message: {}", message);
            
            // Try to parse as a map first
            Map<String, Object> json = gson.fromJson(message, Map.class);
            
            // Check if it's a matchmaking response
            if (json.containsKey("status")) {
                handleMatchmakingMessage(json);
                return;
            }
            
            // Check if it's an error message
            if (json.containsKey("error")) {
                String error = json.get("error").toString();
                logger.warn("Received error from server: {}", error);
                if (listener != null) {
                    listener.onError(error);
                }
                return;
            }
            
            // Check if it's a game state (has sessionId, gameState, etc.)
            if (json.containsKey("sessionId") || json.containsKey("gameState")) {
                handleGameStateMessage(message);
                return;
            }
            
            logger.warn("Unknown message format: {}", message);
            
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse message as JSON", e);
        } catch (Exception e) {
            logger.error("Failed to handle message", e);
        }
    }
    
    private void handleMatchmakingMessage(Map<String, Object> json) {
        String status = json.get("status").toString();
        
        if ("waiting".equals(status)) {
            Number queueSizeNum = (Number) json.get("queueSize");
            int queueSize = queueSizeNum != null ? queueSizeNum.intValue() : 0;
            logger.info("Waiting in queue, size: {}", queueSize);
            if (listener != null) {
                listener.onQueueUpdate(queueSize);
            }
        } else if ("matched".equals(status)) {
            String sessionId = json.get("sessionId").toString();
            logger.info("Match found! Session ID: {}", sessionId);
            if (listener != null) {
                listener.onMatchFound(sessionId);
            }
        }
    }
    
    private void handleGameStateMessage(String message) {
        try {
            GameStateDTO state = gson.fromJson(message, GameStateDTO.class);
            logger.debug("Game state updated - session: {}, state: {}", 
                        state.getSessionId(), state.getGameState());
            if (listener != null) {
                listener.onGameStateUpdate(state);
            }
        } catch (Exception e) {
            logger.error("Failed to parse game state", e);
            if (listener != null) {
                listener.onError("Failed to parse game state: " + e.getMessage());
            }
        }
    }
}