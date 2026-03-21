package com.sweepgame.server.service;

import com.sweepgame.server.model.GameSession;
import com.sweepgame.server.model.PlayerConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceTest {

    @Mock
    private GameSessionManager gameSessionManager;

    @InjectMocks
    private MatchmakingService matchmakingService;

    private PlayerConnection player1;
    private PlayerConnection player2;
    private PlayerConnection player3;

    @BeforeEach
    void setUp() {
        player1 = new PlayerConnection("alice", 1L, "ws-1");
        player2 = new PlayerConnection("bob",   2L, "ws-2");
        player3 = new PlayerConnection("carol", 3L, "ws-3");
    }

    // -------------------------------------------------------------------------
    // joinQueue â€” casual, no match yet
    // -------------------------------------------------------------------------

    @Test
    void joinCasualQueue_withOnePlayer_returnsNull() {
        String result = matchmakingService.joinQueue(player1, false);

        assertNull(result, "Should return null when fewer than 3 players are queued");
        assertEquals(1, matchmakingService.getQueueSize(false));
        verifyNoInteractions(gameSessionManager);
    }

    @Test
    void joinCasualQueue_withTwoPlayers_returnsNull() {
        matchmakingService.joinQueue(player1, false);
        String result = matchmakingService.joinQueue(player2, false);

        assertNull(result);
        assertEquals(2, matchmakingService.getQueueSize(false));
        verifyNoInteractions(gameSessionManager);
    }

    // -------------------------------------------------------------------------
    // joinQueue â€” casual, match formed
    // -------------------------------------------------------------------------

    @Test
    void joinCasualQueue_withThreePlayers_formsMatch() {
        GameSession session = new GameSession(false);
        when(gameSessionManager.createSession(false)).thenReturn(session);

        matchmakingService.joinQueue(player1, false);
        matchmakingService.joinQueue(player2, false);
        String result = matchmakingService.joinQueue(player3, false);

        assertNotNull(result, "Should return a session ID when 3 players are queued");
        assertEquals(session.getSessionId(), result);
        verify(gameSessionManager).createSession(false);
        verify(gameSessionManager, times(3)).addPlayerToSession(eq(session.getSessionId()), any(PlayerConnection.class));
    }

    @Test
    void joinCasualQueue_withThreePlayers_drainsCasualQueue() {
        GameSession session = new GameSession(false);
        when(gameSessionManager.createSession(false)).thenReturn(session);

        matchmakingService.joinQueue(player1, false);
        matchmakingService.joinQueue(player2, false);
        matchmakingService.joinQueue(player3, false);

        assertEquals(0, matchmakingService.getQueueSize(false));
    }

    // -------------------------------------------------------------------------
    // joinQueue â€” ranked queue is independent of casual
    // -------------------------------------------------------------------------

    @Test
    void joinRankedQueue_withThreePlayers_formsRankedMatch() {
        GameSession session = new GameSession(true);
        when(gameSessionManager.createSession(true)).thenReturn(session);

        matchmakingService.joinQueue(player1, true);
        matchmakingService.joinQueue(player2, true);
        String result = matchmakingService.joinQueue(player3, true);

        assertNotNull(result);
        assertEquals(session.getSessionId(), result);
        verify(gameSessionManager).createSession(true);
    }

    @Test
    void rankedAndCasualQueues_areIndependent() {
        // Two players in casual, one in ranked â€” neither queue should fire
        matchmakingService.joinQueue(player1, false);
        matchmakingService.joinQueue(player2, false);
        String result = matchmakingService.joinQueue(player3, true);

        assertNull(result);
        assertEquals(2, matchmakingService.getQueueSize(false));
        assertEquals(1, matchmakingService.getQueueSize(true));
        verifyNoInteractions(gameSessionManager);
    }

    // -------------------------------------------------------------------------
    // joinQueue â€” all three players added to session in order
    // -------------------------------------------------------------------------

    @Test
    void joinCasualQueue_addsPlayersToSessionInQueueOrder() {
        GameSession session = new GameSession(false);
        when(gameSessionManager.createSession(false)).thenReturn(session);

        matchmakingService.joinQueue(player1, false);
        matchmakingService.joinQueue(player2, false);
        matchmakingService.joinQueue(player3, false);

        ArgumentCaptor<PlayerConnection> captor = ArgumentCaptor.forClass(PlayerConnection.class);
        verify(gameSessionManager, times(3)).addPlayerToSession(eq(session.getSessionId()), captor.capture());

        List<PlayerConnection> added = captor.getAllValues();
        assertEquals("alice", added.get(0).getUsername());
        assertEquals("bob",   added.get(1).getUsername());
        assertEquals("carol", added.get(2).getUsername());
    }

    // -------------------------------------------------------------------------
    // joinQueue â€” fourth player after a match starts a new wait
    // -------------------------------------------------------------------------

    @Test
    void joinCasualQueue_fourthPlayer_waitsForNextMatch() {
        GameSession session = new GameSession(false);
        when(gameSessionManager.createSession(false)).thenReturn(session);

        matchmakingService.joinQueue(player1, false);
        matchmakingService.joinQueue(player2, false);
        matchmakingService.joinQueue(player3, false);   // match formed, queue drained

        PlayerConnection player4 = new PlayerConnection("dave", 4L, "ws-4");
        String result = matchmakingService.joinQueue(player4, false);

        assertNull(result, "Fourth player alone should not form a new match");
        assertEquals(1, matchmakingService.getQueueSize(false));
    }

    // -------------------------------------------------------------------------
    // leaveQueue
    // -------------------------------------------------------------------------

    @Test
    void leaveQueue_removesPlayerFromCasualQueue() {
        matchmakingService.joinQueue(player1, false);
        matchmakingService.joinQueue(player2, false);

        matchmakingService.leaveQueue("alice");

        assertEquals(1, matchmakingService.getQueueSize(false));
        assertFalse(matchmakingService.getQueuePlayers(false)
                        .stream().anyMatch(p -> p.getUsername().equals("alice")));
    }

    @Test
    void leaveQueue_removesPlayerFromRankedQueue() {
        matchmakingService.joinQueue(player1, true);

        matchmakingService.leaveQueue("alice");

        assertEquals(0, matchmakingService.getQueueSize(true));
    }

    @Test
    void leaveQueue_unknownUsername_doesNothing() {
        matchmakingService.joinQueue(player1, false);

        assertDoesNotThrow(() -> matchmakingService.leaveQueue("nobody"));
        assertEquals(1, matchmakingService.getQueueSize(false));
    }

    @Test
    void leaveQueue_removesFromBothQueuesSimultaneously() {
        // Edge case: same username in both queues should be cleaned from both
        // BUG FOUND: nothing prevents the same PlayerConnection (or username) from
        //            being added to both queues; leaveQueue correctly handles this,
        //            but joinQueue offers no guard against duplicate registrations.
        matchmakingService.joinQueue(player1, false);
        matchmakingService.joinQueue(player1, true);

        matchmakingService.leaveQueue("alice");

        assertEquals(0, matchmakingService.getQueueSize(false));
        assertEquals(0, matchmakingService.getQueueSize(true));
    }

    // -------------------------------------------------------------------------
    // getQueueSize / getTotalQueueSize
    // -------------------------------------------------------------------------

    @Test
    void getQueueSize_returnsCorrectCounts() {
        matchmakingService.joinQueue(player1, false);
        matchmakingService.joinQueue(player2, true);

        assertEquals(1, matchmakingService.getQueueSize(false));
        assertEquals(1, matchmakingService.getQueueSize(true));
    }

    @Test
    void getTotalQueueSize_sumsBothQueues() {
        matchmakingService.joinQueue(player1, false);
        matchmakingService.joinQueue(player2, true);
        matchmakingService.joinQueue(player3, true);

        assertEquals(3, matchmakingService.getTotalQueueSize());
    }

    @Test
    void getTotalQueueSize_emptyQueues_returnsZero() {
        assertEquals(0, matchmakingService.getTotalQueueSize());
    }

    // -------------------------------------------------------------------------
    // getQueuePlayers
    // -------------------------------------------------------------------------

    @Test
    void getQueuePlayers_returnsDefensiveCopy() {
        matchmakingService.joinQueue(player1, false);
        matchmakingService.joinQueue(player2, false);

        List<PlayerConnection> snapshot = matchmakingService.getQueuePlayers(false);
        assertEquals(2, snapshot.size());

        // Mutating the returned list must not affect the internal queue
        snapshot.clear();
        assertEquals(2, matchmakingService.getQueueSize(false));
    }

    @Test
    void getQueuePlayers_emptyQueue_returnsEmptyList() {
        assertTrue(matchmakingService.getQueuePlayers(false).isEmpty());
        assertTrue(matchmakingService.getQueuePlayers(true).isEmpty());
    }
}
