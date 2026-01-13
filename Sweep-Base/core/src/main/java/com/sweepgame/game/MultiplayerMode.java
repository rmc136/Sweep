package com.sweepgame.game;

import com.badlogic.gdx.Gdx;
import com.sweepgame.game.Card;
import com.sweepgame.game.Player;
import com.sweepgame.network.GameStateDTO;
import com.sweepgame.network.PlayerStateDTO;
import com.sweepgame.network.WebSocketManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Multiplayer game mode using WebSocket connection
 */
public class MultiplayerMode implements GameMode, WebSocketManager.GameStateListener {
    private static final Logger logger = LoggerFactory.getLogger(MultiplayerMode.class);

    private final WebSocketManager wsManager;
    private final String myUsername;
    private final String sessionId;

    private List<Player> players;
    private List<Card> tableCards;
    private Player currentPlayer;
    private int myPlayerIndex = -1;
    private int currentPlayerIndex = 0;
    private List<Card> lastCollected;
    private boolean gameOver = false;
    private Player winner;

    public MultiplayerMode(WebSocketManager wsManager, String sessionId, String myUsername) {
        this.wsManager = wsManager;
        this.sessionId = sessionId;
        this.myUsername = myUsername;
        this.players = new ArrayList<>();
        this.tableCards = new ArrayList<>();
        this.lastCollected = new ArrayList<>();

        wsManager.setListener(this);
        logger.info("MultiplayerMode initialized for session: {}", sessionId);
    }

    @Override
    public void startGame(int startingPlayerIndex) {
        // Game is started by server, just wait for updates
        logger.info("Waiting for server to start game");
    }

    @Override
    public List<Player> getPlayers() {
        return players;
    }

    @Override
    public List<Card> getTableCards() {
        return tableCards;
    }

    @Override
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    @Override
    public void playCard(Player player, Card handCard, List<Card> selectedTableCards) {
        // Convert to indices and send to server
        int handIndex = player.getHand().indexOf(handCard);
        List<Integer> tableIndices = new ArrayList<>();
        for (Card card : selectedTableCards) {
            tableIndices.add(tableCards.indexOf(card));
        }

        wsManager.playCard(handIndex, tableIndices);
        logger.info("Sent move to server: hand={}, table={}", handIndex, tableIndices);
    }

    @Override
    public boolean allHandsEmpty() {
        for (Player p : players) {
            if (!p.getHand().isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public void dealNewRound() {
        // Server handles dealing
    }

    @Override
    public boolean isGameOver() {
        return gameOver;
    }

    @Override
    public Player getWinner() {
        return winner;
    }

    @Override
    public void finishGame() {
        // Server handles finishing
    }

    @Override
    public List<Card> getLastCollectedCards() {
        return lastCollected;
    }

    @Override
    public boolean isMultiplayer() {
        return true;
    }

    @Override
    public boolean isMyTurn() {
        return myPlayerIndex != -1 && currentPlayerIndex == myPlayerIndex;
    }

    // WebSocket callbacks

    public void updateState(GameStateDTO state) {
        onGameStateUpdate(state);
    }

    @Override
    public void onGameStateUpdate(GameStateDTO state) {
        logger.info("Game state update received");

        // Update players first to ensure lists are populated
        if (state.getPlayers() != null) {
            updatePlayers(state.getPlayers());
        }

        // Update current player
        currentPlayerIndex = state.getCurrentPlayerIndex();
        if (currentPlayerIndex >= 0 && currentPlayerIndex < players.size()) {
            currentPlayer = players.get(currentPlayerIndex);
        }

        // Check for game over
        if ("FINISHED".equals(state.getGameState())) {
            gameOver = true;
            // Winner is determined by highest points
            winner = players.stream()
                    .max((p1, p2) -> Integer.compare(p1.calculatePoints(), p2.calculatePoints()))
                    .orElse(null);
        }
    }

    private void updatePlayers(List<PlayerStateDTO> playerStates) {
        // Initialize players if needed
        if (players.isEmpty()) {
            for (PlayerStateDTO state : playerStates) {
                Player player = new Player(state.getUsername());
                players.add(player);

                if (state.getUsername().equals(myUsername)) {
                    myPlayerIndex = players.size() - 1;
                    logger.info("My player index: {}", myPlayerIndex);
                }
            }
        }

        // Update player states (points, sweeps, etc.)
        for (int i = 0; i < playerStates.size() && i < players.size(); i++) {
            PlayerStateDTO state = playerStates.get(i);
            Player player = players.get(i);

            // Note: We can't update hand/collected cards directly from server
            // as they're not sent for security reasons
            // We only update what's visible: points and sweeps
        }
    }

    @Override
    public void onConnect() {
        // Already connected when this mode starts
    }

    @Override
    public void onMatchFound(String sessionId) {
        // Not used in game mode
    }

    @Override
    public void onQueueUpdate(int queueSize) {
        // Not used in game mode
    }

    @Override
    public void onError(String message) {
        logger.error("Multiplayer error: {}", message);
    }

    @Override
    public void onDisconnect() {
        logger.warn("Disconnected from server");
    }
}
