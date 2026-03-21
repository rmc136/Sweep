package com.sweepgame.server.service;

import com.sweepgame.server.model.GameSession;
import com.sweepgame.server.model.PlayerConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionManagerTest {

    private GameSessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new GameSessionManager();
    }

    // â”€â”€ createSession â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void createSession_ranked_returnsSessionInWaitingState() {
        GameSession session = manager.createSession(true);

        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertTrue(session.isRanked());
        assertEquals(GameSession.GameState.WAITING, session.getState());
    }

    @Test
    void createSession_unranked_storesSessionAndIncrementsCount() {
        assertEquals(0, manager.getActiveSessionCount());
        manager.createSession(false);
        assertEquals(1, manager.getActiveSessionCount());
    }

    @Test
    void createSession_multipleSessions_eachHasUniqueId() {
        GameSession s1 = manager.createSession(false);
        GameSession s2 = manager.createSession(false);

        assertNotEquals(s1.getSessionId(), s2.getSessionId());
        assertEquals(2, manager.getActiveSessionCount());
    }

    // â”€â”€ getSession â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void getSession_existingId_returnsSession() {
        GameSession created = manager.createSession(false);
        GameSession found = manager.getSession(created.getSessionId());
        assertSame(created, found);
    }

    @Test
    void getSession_unknownId_returnsNull() {
        assertNull(manager.getSession("nonexistent-id"));
    }

    // â”€â”€ getSessionByPlayer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void getSessionByPlayer_afterAddingPlayer_returnsCorrectSession() {
        GameSession session = manager.createSession(false);
        PlayerConnection player = new PlayerConnection("alice", 1L, "ws-1");

        manager.addPlayerToSession(session.getSessionId(), player);

        assertSame(session, manager.getSessionByPlayer("alice"));
    }

    @Test
    void getSessionByPlayer_unknownPlayer_returnsNull() {
        assertNull(manager.getSessionByPlayer("ghost"));
    }

    // â”€â”€ addPlayerToSession â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void addPlayerToSession_validSession_assignsSessionIdToPlayer() {
        GameSession session = manager.createSession(false);
        PlayerConnection player = new PlayerConnection("bob", 2L, "ws-2");

        manager.addPlayerToSession(session.getSessionId(), player);

        assertEquals(session.getSessionId(), player.getSessionId());
        assertEquals(1, manager.getActivePlayers());
    }

    @Test
    void addPlayerToSession_threePlayersAdded_sessionBecomesReady() {
        GameSession session = manager.createSession(false);
        addThreePlayers(session.getSessionId());

        assertEquals(GameSession.GameState.READY, session.getState());
        assertEquals(3, manager.getActivePlayers());
    }

    @Test
    void addPlayerToSession_sessionNotFound_throwsIllegalArgument() {
        PlayerConnection player = new PlayerConnection("carol", 3L, "ws-3");
        assertThrows(IllegalArgumentException.class,
                () -> manager.addPlayerToSession("bad-id", player));
    }

    @Test
    void addPlayerToSession_fourthPlayer_throwsIllegalState() {
        GameSession session = manager.createSession(false);
        addThreePlayers(session.getSessionId());

        PlayerConnection fourth = new PlayerConnection("dave", 4L, "ws-4");
        assertThrows(IllegalStateException.class,
                () -> manager.addPlayerToSession(session.getSessionId(), fourth));
    }

    // â”€â”€ removePlayerFromSession â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void removePlayerFromSession_removesPlayerMapping() {
        GameSession session = manager.createSession(false);
        PlayerConnection player = new PlayerConnection("eve", 5L, "ws-5");
        manager.addPlayerToSession(session.getSessionId(), player);

        manager.removePlayerFromSession("eve");

        assertNull(manager.getSessionByPlayer("eve"));
        assertEquals(0, manager.getActivePlayers());
    }

    @Test
    void removePlayerFromSession_lastPlayer_removesSessionToo() {
        GameSession session = manager.createSession(false);
        PlayerConnection player = new PlayerConnection("frank", 6L, "ws-6");
        manager.addPlayerToSession(session.getSessionId(), player);
        String sessionId = session.getSessionId();

        manager.removePlayerFromSession("frank");

        assertNull(manager.getSession(sessionId));
        assertEquals(0, manager.getActiveSessionCount());
    }

    @Test
    void removePlayerFromSession_remainingPlayers_sessionStaysAlive() {
        GameSession session = manager.createSession(false);
        manager.addPlayerToSession(session.getSessionId(), new PlayerConnection("g1", 7L, "ws-7"));
        manager.addPlayerToSession(session.getSessionId(), new PlayerConnection("g2", 8L, "ws-8"));

        manager.removePlayerFromSession("g1");

        assertNotNull(manager.getSession(session.getSessionId()));
        assertEquals(1, manager.getActivePlayers());
    }

    @Test
    void removePlayerFromSession_unknownPlayer_noOp() {
        assertEquals(0, manager.getActivePlayers());
        // Should not throw
        manager.removePlayerFromSession("nobody");
        assertEquals(0, manager.getActivePlayers());
    }

    // â”€â”€ startSession â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void startSession_readySession_transitionsToInProgress() {
        GameSession session = manager.createSession(false);
        addThreePlayers(session.getSessionId());

        manager.startSession(session.getSessionId());

        assertEquals(GameSession.GameState.IN_PROGRESS, session.getState());
        assertTrue(session.isActive());
    }

    @Test
    void startSession_sessionNotFound_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.startSession("bad-id"));
    }

    @Test
    void startSession_sessionNotReady_throwsIllegalState() {
        GameSession session = manager.createSession(false);
        // Only 1 player â€“ still WAITING
        manager.addPlayerToSession(session.getSessionId(),
                new PlayerConnection("h1", 9L, "ws-9"));

        assertThrows(IllegalStateException.class,
                () -> manager.startSession(session.getSessionId()));
    }

    // â”€â”€ finishSession â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void finishSession_activeSession_removesSessionAndPlayerMappings() {
        GameSession session = manager.createSession(false);
        addThreePlayers(session.getSessionId());
        manager.startSession(session.getSessionId());
        String sessionId = session.getSessionId();

        manager.finishSession(sessionId);

        assertNull(manager.getSession(sessionId));
        assertEquals(0, manager.getActiveSessionCount());
        assertEquals(0, manager.getActivePlayers());
    }

    @Test
    void finishSession_sessionNotFound_noOp() {
        // Should not throw
        manager.finishSession("nonexistent");
        assertEquals(0, manager.getActiveSessionCount());
    }

    @Test
    void finishSession_setsStateToFinished() {
        GameSession session = manager.createSession(false);
        addThreePlayers(session.getSessionId());
        manager.startSession(session.getSessionId());

        manager.finishSession(session.getSessionId());

        assertEquals(GameSession.GameState.FINISHED, session.getState());
    }

    // â”€â”€ counters â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void getActiveSessionCount_reflectsCreateAndFinish() {
        GameSession s1 = manager.createSession(false);
        manager.createSession(false);
        assertEquals(2, manager.getActiveSessionCount());

        // finish s1 without players â†’ uses no-op path in finishSession (no players to iterate)
        manager.finishSession(s1.getSessionId());
        assertEquals(1, manager.getActiveSessionCount());
    }

    @Test
    void getActivePlayers_reflectsAddAndRemove() {
        GameSession session = manager.createSession(false);
        manager.addPlayerToSession(session.getSessionId(), new PlayerConnection("p1", 1L, "w1"));
        manager.addPlayerToSession(session.getSessionId(), new PlayerConnection("p2", 2L, "w2"));
        assertEquals(2, manager.getActivePlayers());

        manager.removePlayerFromSession("p1");
        assertEquals(1, manager.getActivePlayers());
    }

    // â”€â”€ helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void addThreePlayers(String sessionId) {
        manager.addPlayerToSession(sessionId, new PlayerConnection("p1", 1L, "ws-p1"));
        manager.addPlayerToSession(sessionId, new PlayerConnection("p2", 2L, "ws-p2"));
        manager.addPlayerToSession(sessionId, new PlayerConnection("p3", 3L, "ws-p3"));
    }
}
