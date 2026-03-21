package com.sweepgame.server.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession(false);
    }

    // â”€â”€ constructor â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void constructor_initialState_isWaiting() {
        assertEquals(GameSession.GameState.WAITING, session.getState());
    }

    @Test
    void constructor_generatesNonNullSessionId() {
        assertNotNull(session.getSessionId());
        assertFalse(session.getSessionId().isBlank());
    }

    @Test
    void constructor_playersListIsEmpty() {
        assertTrue(session.getPlayers().isEmpty());
    }

    @Test
    void constructor_createdAtIsSet() {
        assertNotNull(session.getCreatedAt());
    }

    @Test
    void constructor_ranked_flagIsPreserved() {
        GameSession ranked = new GameSession(true);
        assertTrue(ranked.isRanked());
        assertFalse(session.isRanked());
    }

    // â”€â”€ addPlayer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void addPlayer_firstPlayer_setsSessionIdOnPlayer() {
        PlayerConnection player = new PlayerConnection("alice", 1L, "ws-1");
        session.addPlayer(player);

        assertEquals(session.getSessionId(), player.getSessionId());
        assertEquals(1, session.getPlayers().size());
    }

    @Test
    void addPlayer_stateRemainsWaiting_untilThirdPlayer() {
        session.addPlayer(new PlayerConnection("a", 1L, "ws-a"));
        assertEquals(GameSession.GameState.WAITING, session.getState());

        session.addPlayer(new PlayerConnection("b", 2L, "ws-b"));
        assertEquals(GameSession.GameState.WAITING, session.getState());
    }

    @Test
    void addPlayer_thirdPlayer_transitionsToReady() {
        addThreePlayers();
        assertEquals(GameSession.GameState.READY, session.getState());
    }

    @Test
    void addPlayer_fourthPlayer_throwsIllegalState() {
        addThreePlayers();
        assertThrows(IllegalStateException.class,
                () -> session.addPlayer(new PlayerConnection("d", 4L, "ws-d")));
    }

    @Test
    void isFull_afterThreePlayers_returnsTrue() {
        assertFalse(session.isFull());
        addThreePlayers();
        assertTrue(session.isFull());
    }

    // â”€â”€ removePlayer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void removePlayer_existingPlayer_removesFromList() {
        session.addPlayer(new PlayerConnection("alice", 1L, "ws-1"));
        session.removePlayer("alice");

        assertTrue(session.getPlayers().isEmpty());
    }

    @Test
    void removePlayer_unknownUsername_noOp() {
        session.addPlayer(new PlayerConnection("alice", 1L, "ws-1"));
        session.removePlayer("ghost");

        assertEquals(1, session.getPlayers().size());
    }

    @Test
    void removePlayer_duringInProgress_transitionsToFinished() {
        addThreePlayers();
        session.startGame();
        assertEquals(GameSession.GameState.IN_PROGRESS, session.getState());

        session.removePlayer("p1");

        assertEquals(GameSession.GameState.FINISHED, session.getState());
    }

    @Test
    void removePlayer_duringWaiting_stateRemainsWaiting() {
        session.addPlayer(new PlayerConnection("p1", 1L, "ws-1"));
        session.removePlayer("p1");

        assertEquals(GameSession.GameState.WAITING, session.getState());
    }

    // â”€â”€ startGame â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void startGame_readySession_transitionsToInProgress() {
        addThreePlayers();
        session.startGame();

        assertEquals(GameSession.GameState.IN_PROGRESS, session.getState());
        assertTrue(session.isActive());
        assertNotNull(session.getStartedAt());
    }

    @Test
    void startGame_readySession_initializesGameLogic() {
        addThreePlayers();
        session.startGame();

        assertNotNull(session.getGameLogic());
    }

    @Test
    void startGame_inWaitingState_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> session.startGame());
    }

    @Test
    void startGame_alreadyInProgress_throwsIllegalState() {
        addThreePlayers();
        session.startGame();

        assertThrows(IllegalStateException.class, () -> session.startGame());
    }

    // â”€â”€ finishGame â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void finishGame_activeGame_transitionsToFinished() {
        addThreePlayers();
        session.startGame();
        session.finishGame();

        assertEquals(GameSession.GameState.FINISHED, session.getState());
        assertFalse(session.isActive());
        assertNotNull(session.getFinishedAt());
    }

    @Test
    void finishGame_beforeGameStarted_setsFinishedState() {
        // gameLogic is null â€“ finishGame must handle that gracefully
        session.finishGame();

        assertEquals(GameSession.GameState.FINISHED, session.getState());
    }

    // â”€â”€ isActive â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void isActive_beforeStart_returnsFalse() {
        assertFalse(session.isActive());
    }

    @Test
    void isActive_afterStart_returnsTrue() {
        addThreePlayers();
        session.startGame();
        assertTrue(session.isActive());
    }

    // â”€â”€ getPlayerByUsername â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void getPlayerByUsername_existingPlayer_returnsCorrectPlayer() {
        PlayerConnection alice = new PlayerConnection("alice", 1L, "ws-1");
        session.addPlayer(alice);

        assertSame(alice, session.getPlayerByUsername("alice"));
    }

    @Test
    void getPlayerByUsername_unknownPlayer_returnsNull() {
        assertNull(session.getPlayerByUsername("ghost"));
    }

    // â”€â”€ getPlayerIndex â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void getPlayerIndex_existingPlayer_returnsCorrectIndex() {
        session.addPlayer(new PlayerConnection("p1", 1L, "ws-1"));
        session.addPlayer(new PlayerConnection("p2", 2L, "ws-2"));

        assertEquals(0, session.getPlayerIndex("p1"));
        assertEquals(1, session.getPlayerIndex("p2"));
    }

    @Test
    void getPlayerIndex_unknownPlayer_returnsMinusOne() {
        assertEquals(-1, session.getPlayerIndex("ghost"));
    }

    // â”€â”€ getGamePlayer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void getGamePlayer_beforeGameStart_returnsNull() {
        assertNull(session.getGamePlayer(0));
    }

    @Test
    void getGamePlayer_negativeIndex_returnsNull() {
        addThreePlayers();
        session.startGame();

        assertNull(session.getGamePlayer(-1));
    }

    @Test
    void getGamePlayer_outOfBoundsIndex_returnsNull() {
        addThreePlayers();
        session.startGame();

        assertNull(session.getGamePlayer(99));
    }

    @Test
    void getGamePlayer_validIndex_returnsPlayer() {
        addThreePlayers();
        session.startGame();

        assertNotNull(session.getGamePlayer(0));
        assertNotNull(session.getGamePlayer(1));
        assertNotNull(session.getGamePlayer(2));
    }

    // â”€â”€ helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void addThreePlayers() {
        session.addPlayer(new PlayerConnection("p1", 1L, "ws-p1"));
        session.addPlayer(new PlayerConnection("p2", 2L, "ws-p2"));
        session.addPlayer(new PlayerConnection("p3", 3L, "ws-p3"));
    }
}
