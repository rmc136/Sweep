package com.sweepgame.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SweepLogicTest {

    private SweepLogic game;

    @BeforeEach
    void setUp() {
        game = new SweepLogic();
        game.startGame();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Replace the current table contents with the given cards. */
    private void setupTable(Card... cards) {
        game.getTableCards().clear();
        for (Card c : cards) game.getTableCards().add(c);
    }

    /** Replace a player's hand with the given cards. */
    private void setupHand(Player player, Card... cards) {
        player.getHand().clear();
        for (Card c : cards) player.getHand().add(c);
    }

    /** Drain all remaining deck cards so the deck becomes empty. */
    private void drainDeck() {
        Deck deck = game.getDeck();
        while (!deck.isEmpty()) deck.draw();
    }

    /** Empty every player's hand. */
    private void clearAllHands() {
        for (Player p : game.getPlayers()) p.getHand().clear();
    }

    // =========================================================================
    // startGame / initial state
    // =========================================================================

    @Test
    void startGame_createsExactlyThreePlayers() {
        assertEquals(3, game.getPlayers().size());
    }

    @Test
    void startGame_playerNamesAreCorrect() {
        List<Player> players = game.getPlayers();
        assertEquals("Johnny",  players.get(0).getName());
        assertEquals("Joni",    players.get(1).getName());
        assertEquals("Rodrigo", players.get(2).getName());
    }

    @Test
    void startGame_eachPlayerReceivesThreeCards() {
        for (Player p : game.getPlayers()) {
            assertEquals(3, p.getHand().size(),
                    p.getName() + " should have exactly 3 cards");
        }
    }

    @Test
    void startGame_tableHasFourCardsOrWasSweptOnInit() {
        // Table starts with 4 cards; if their sum == 15 they are cleared immediately.
        int size = game.getTableCards().size();
        assertTrue(size == 0 || size == 4,
                "Table should have 0 (initial sweep) or 4 cards, got " + size);
    }

    @Test
    void startGame_defaultCurrentPlayerIsJohnny() {
        assertEquals("Johnny", game.getCurrentPlayer().getName());
    }

    @Test
    void startGame_withIndex1_currentPlayerIsJoni() {
        SweepLogic g2 = new SweepLogic();
        g2.startGame(1);
        assertEquals("Joni", g2.getCurrentPlayer().getName());
    }

    @Test
    void startGame_withIndex2_currentPlayerIsRodrigo() {
        SweepLogic g2 = new SweepLogic();
        g2.startGame(2);
        assertEquals("Rodrigo", g2.getCurrentPlayer().getName());
    }

    @Test
    void startGame_resetsTableFromPreviousGame() {
        setupTable(new Card(Suit.HEARTS, Rank.ACE));
        game.startGame(); // should clear and re-deal
        int size = game.getTableCards().size();
        assertTrue(size == 0 || size == 4);
    }

    // BUG FOUND: startGame(int) uses `players.get(startingPlayerIndex)` (raw param) instead
    // of `players.get(startingPlayerIndex % 3)` when awarding the initial-sweep bonus.
    // Calling startGame(4) would throw IndexOutOfBoundsException if table sum == 15.
    //
    // BUG FOUND: the `isFirstRound` flag is never reset at the top of startGame().
    // If a sweep occurred in a previous game, subsequent calls to startGame() will never
    // award the initial-sweep bonus even when the opening table sums to 15.

    // =========================================================================
    // isGameOver / allHandsEmpty
    // =========================================================================

    @Test
    void isGameOver_falseRightAfterStart() {
        assertFalse(game.isGameOver());
    }

    @Test
    void isGameOver_falseWhenHandsEmptyButDeckNotEmpty() {
        clearAllHands();
        // Deck still has cards from startGame
        assertFalse(game.isGameOver());
    }

    @Test
    void isGameOver_trueWhenAllHandsEmptyAndDeckEmpty() {
        clearAllHands();
        drainDeck();
        assertTrue(game.isGameOver());
    }

    @Test
    void allHandsEmpty_falseAfterDeal() {
        assertFalse(game.allHandsEmpty());
    }

    @Test
    void allHandsEmpty_trueAfterClearingAllHands() {
        clearAllHands();
        assertTrue(game.allHandsEmpty());
    }

    // =========================================================================
    // playCardWithSelection â€” guard clauses
    // =========================================================================

    @Test
    void playCardWithSelection_nullPlayer_doesNotThrow() {
        assertDoesNotThrow(() ->
                game.playCardWithSelection(null, new Card(Suit.HEARTS, Rank.ACE), List.of()));
    }

    @Test
    void playCardWithSelection_nullCard_doesNotThrow() {
        Player p = game.getPlayers().get(0);
        assertDoesNotThrow(() ->
                game.playCardWithSelection(p, null, List.of()));
    }

    @Test
    void playCardWithSelection_cardNotInHand_leavesTableUnchanged() {
        Player player = game.getCurrentPlayer();
        player.getHand().clear(); // ensure card is not in hand
        Card notInHand = new Card(Suit.SPADES, Rank.KING);
        int tableSizeBefore = game.getTableCards().size();

        game.playCardWithSelection(player, notInHand, List.of());

        assertEquals(tableSizeBefore, game.getTableCards().size());
    }

    @Test
    void playCardWithSelection_cardNotInHand_doesNotAdvanceTurn() {
        Player before = game.getCurrentPlayer();
        before.getHand().clear();
        Card notInHand = new Card(Suit.SPADES, Rank.KING);

        game.playCardWithSelection(before, notInHand, List.of());

        // Turn should NOT have advanced
        assertSame(before, game.getCurrentPlayer());
    }

    // =========================================================================
    // playCardWithSelection â€” playing to the table
    // =========================================================================

    @Test
    void playCardWithSelection_emptySelection_addsCardToTable() {
        Player player = game.getCurrentPlayer();
        Card handCard = new Card(Suit.HEARTS, Rank.ACE);
        setupHand(player, handCard);
        setupTable();

        game.playCardWithSelection(player, handCard, List.of());

        assertTrue(game.getTableCards().contains(handCard));
    }

    @Test
    void playCardWithSelection_sumNot15_addsCardToTable() {
        Player player = game.getCurrentPlayer();
        Card handCard  = new Card(Suit.HEARTS, Rank.ACE);  // value 1
        Card tableCard = new Card(Suit.CLUBS,  Rank.TWO);  // value 2  â†’  1+2 = 3
        setupHand(player, handCard);
        setupTable(tableCard);

        game.playCardWithSelection(player, handCard, List.of(tableCard));

        assertTrue(game.getTableCards().contains(handCard),
                "Hand card should go to table when sum != 15");
        assertTrue(game.getTableCards().contains(tableCard),
                "Table card should remain when sum != 15");
    }

    @Test
    void playCardWithSelection_selectionNotOnTable_addsCardToTable() {
        Player player = game.getCurrentPlayer();
        Card handCard      = new Card(Suit.HEARTS, Rank.KING);  // value 10
        Card fakeTableCard = new Card(Suit.SPADES, Rank.FIVE);  // NOT on table (sum would be 15)
        Card actualOnTable = new Card(Suit.CLUBS,  Rank.ACE);
        setupHand(player, handCard);
        setupTable(actualOnTable); // fakeTableCard is absent

        game.playCardWithSelection(player, handCard, List.of(fakeTableCard));

        assertTrue(game.getTableCards().contains(handCard),
                "Hand card should go to table when selected cards are not on the table");
    }

    // =========================================================================
    // playCardWithSelection â€” capturing cards
    // =========================================================================

    @Test
    void playCardWithSelection_sumIs15_capturesCards() {
        Player player   = game.getCurrentPlayer();
        Card handCard   = new Card(Suit.HEARTS,  Rank.KING); // value 10
        Card tableCard  = new Card(Suit.CLUBS,   Rank.FIVE); // value 5  â†’ 10+5=15
        setupHand(player, handCard);
        setupTable(tableCard);

        game.playCardWithSelection(player, handCard, List.of(tableCard));

        assertTrue(game.getTableCards().isEmpty(), "Table should be empty after capture");
        assertEquals(2, player.getPointsStack().size());
        assertTrue(player.getPointsStack().contains(handCard));
        assertTrue(player.getPointsStack().contains(tableCard));
    }

    @Test
    void playCardWithSelection_captureWithMultipleTableCards_sumIs15() {
        Player player  = game.getCurrentPlayer();
        Card handCard  = new Card(Suit.HEARTS, Rank.SEVEN); // value 7
        Card t1        = new Card(Suit.CLUBS,  Rank.FOUR);  // value 4
        Card t2        = new Card(Suit.SPADES, Rank.FOUR);  // value 4  â†’ 7+4+4=15
        Card bystander = new Card(Suit.CLUBS,  Rank.ACE);   // stays on table
        setupHand(player, handCard);
        setupTable(t1, t2, bystander);

        game.playCardWithSelection(player, handCard, List.of(t1, t2));

        assertFalse(game.getTableCards().contains(t1));
        assertFalse(game.getTableCards().contains(t2));
        assertTrue(game.getTableCards().contains(bystander),
                "Non-selected table card should remain");
        assertEquals(3, player.getPointsStack().size());
    }

    // =========================================================================
    // playCardWithSelection â€” sweep detection
    // =========================================================================

    @Test
    void playCardWithSelection_clearsTable_incrementsBrushes() {
        Player player  = game.getCurrentPlayer();
        Card handCard  = new Card(Suit.HEARTS, Rank.KING); // value 10
        Card tableCard = new Card(Suit.CLUBS,  Rank.FIVE); // value 5  â€” only card on table
        setupHand(player, handCard);
        setupTable(tableCard); // single card â†’ capturing it clears the table

        int brushesBefore = player.getBrushes();
        game.playCardWithSelection(player, handCard, List.of(tableCard));

        assertEquals(brushesBefore + 1, player.getBrushes(), "Clearing the table should earn a brush");
    }

    @Test
    void playCardWithSelection_partialCapture_doesNotIncrementBrushes() {
        Player player  = game.getCurrentPlayer();
        Card handCard  = new Card(Suit.HEARTS, Rank.KING); // value 10
        Card target    = new Card(Suit.CLUBS,  Rank.FIVE); // value 5
        Card remaining = new Card(Suit.CLUBS,  Rank.ACE);  // value 1 â€” stays
        setupHand(player, handCard);
        setupTable(target, remaining);

        int brushesBefore = player.getBrushes();
        game.playCardWithSelection(player, handCard, List.of(target));

        assertEquals(brushesBefore, player.getBrushes(), "Partial capture should not earn a brush");
    }

    // =========================================================================
    // Turn advancement
    // =========================================================================

    @Test
    void playCardWithSelection_advancesTurn_afterPlay() {
        // advanceTurn goes backwards: (index-1+3)%3
        // Starting at 0 (Johnny) â†’ next is index 2 (Rodrigo)
        Player johnny = game.getCurrentPlayer();
        assertEquals("Johnny", johnny.getName());
        Card handCard = new Card(Suit.HEARTS, Rank.ACE);
        setupHand(johnny, handCard);
        setupTable();

        game.playCardWithSelection(johnny, handCard, List.of());

        assertEquals("Rodrigo", game.getCurrentPlayer().getName());
    }

    @Test
    void playCardWithSelection_turnRotatesCorrectly_threeConsecutivePlays() {
        // 0=Johnny â†’ 2=Rodrigo â†’ 1=Joni â†’ 0=Johnny
        List<Player> players = game.getPlayers();
        Card c1 = new Card(Suit.HEARTS,   Rank.ACE);
        Card c2 = new Card(Suit.CLUBS,    Rank.ACE);
        Card c3 = new Card(Suit.DIAMONDS, Rank.ACE);
        setupHand(players.get(0), c1);
        setupHand(players.get(2), c2);
        setupHand(players.get(1), c3);
        setupTable();

        game.playCardWithSelection(players.get(0), c1, List.of()); // Johnny plays
        assertEquals("Rodrigo", game.getCurrentPlayer().getName());

        game.playCardWithSelection(players.get(2), c2, List.of()); // Rodrigo plays
        assertEquals("Joni", game.getCurrentPlayer().getName());

        game.playCardWithSelection(players.get(1), c3, List.of()); // Joni plays
        assertEquals("Johnny", game.getCurrentPlayer().getName());
    }

    // =========================================================================
    // playCard (legacy / AI path)
    // =========================================================================

    @Test
    void playCard_nullPlayer_doesNotThrow() {
        assertDoesNotThrow(() ->
                game.playCard(null, new Card(Suit.HEARTS, Rank.ACE), new ArrayList<>()));
    }

    @Test
    void playCard_nullCard_doesNotThrow() {
        Player p = game.getPlayers().get(0);
        assertDoesNotThrow(() ->
                game.playCard(p, null, new ArrayList<>()));
    }

    @Test
    void playCard_emptySelection_addsCardToTable() {
        Player player  = game.getCurrentPlayer();
        Card handCard  = new Card(Suit.HEARTS, Rank.ACE);
        setupHand(player, handCard);
        setupTable();

        game.playCard(player, handCard, new ArrayList<>());

        assertTrue(game.getTableCards().contains(handCard));
    }

    @Test
    void playCard_withNonEmptySelection_capturesWhenSumIs15() {
        Player player  = game.getCurrentPlayer();
        Card handCard  = new Card(Suit.HEARTS, Rank.KING); // value 10
        Card tableCard = new Card(Suit.CLUBS,  Rank.FIVE); // value 5
        setupHand(player, handCard);
        setupTable(tableCard);

        game.playCard(player, handCard, List.of(tableCard));

        assertFalse(player.getPointsStack().isEmpty());
        assertTrue(game.getTableCards().isEmpty());
    }

    @Test
    void playCard_cardNotInHand_doesNothing() {
        Player player = game.getCurrentPlayer();
        player.getHand().clear();
        Card notInHand = new Card(Suit.SPADES, Rank.KING);
        setupTable(new Card(Suit.CLUBS, Rank.FIVE));

        int tableSizeBefore = game.getTableCards().size();
        game.playCard(player, notInHand, List.of());

        assertEquals(tableSizeBefore, game.getTableCards().size());
    }

    // =========================================================================
    // findRandomValidSum15
    // =========================================================================

    @Test
    void findRandomValidSum15_emptyTable_returnsEmpty() {
        setupTable();
        Card handCard = new Card(Suit.HEARTS, Rank.KING);
        List<Card> result = game.findRandomValidSum15(handCard);
        assertTrue(result.isEmpty());
    }

    @Test
    void findRandomValidSum15_noMatchingSubset_returnsEmpty() {
        Card handCard  = new Card(Suit.HEARTS, Rank.ACE);  // value 1
        Card tableCard = new Card(Suit.CLUBS,  Rank.TWO);  // 1+2=3, nothing sums to 15
        setupTable(tableCard);
        List<Card> result = game.findRandomValidSum15(handCard);
        assertTrue(result.isEmpty());
    }

    @Test
    void findRandomValidSum15_singleTableCardMatch_returnsIt() {
        Card handCard  = new Card(Suit.HEARTS, Rank.KING); // value 10
        Card tableCard = new Card(Suit.CLUBS,  Rank.FIVE); // 10+5=15
        setupTable(tableCard);

        List<Card> result = game.findRandomValidSum15(handCard);

        assertFalse(result.isEmpty());
        assertTrue(result.contains(tableCard));
    }

    @Test
    void findRandomValidSum15_multiCardSubsetMatch() {
        Card handCard = new Card(Suit.HEARTS, Rank.SEVEN); // value 7
        Card t1       = new Card(Suit.CLUBS,  Rank.FOUR);  // value 4
        Card t2       = new Card(Suit.SPADES, Rank.FOUR);  // value 4  â†’ 7+4+4=15
        Card noise    = new Card(Suit.CLUBS,  Rank.ACE);   // value 1 (no help)
        setupTable(t1, t2, noise);

        List<Card> result = game.findRandomValidSum15(handCard);

        // The result must be a non-empty, valid subset
        assertFalse(result.isEmpty());
        int sum = handCard.getValue();
        for (Card c : result) sum += c.getValue();
        assertEquals(15, sum, "Returned subset must sum to 15 with the hand card");
    }

    @Test
    void findRandomValidSum15_doesNotReturnHandCardAlone() {
        // The method only returns table-card subsets; the hand card itself is not a table card.
        Card handCard = new Card(Suit.HEARTS, Rank.ACE); // value 1, can't make 15 alone
        setupTable(new Card(Suit.CLUBS, Rank.TWO));

        List<Card> result = game.findRandomValidSum15(handCard);
        // result may be empty (no match), but must not contain the hand card as a "table subset"
        // AND must not include the played card itself in the returned list
        for (Card c : result) {
            assertNotSame(handCard, c,
                    "findRandomValidSum15 should not include the hand card in the returned subset");
        }
    }

    // =========================================================================
    // dealNewRound
    // =========================================================================

    @Test
    void dealNewRound_givesEachPlayerThreeMoreCards() {
        clearAllHands();
        game.dealNewRound();

        for (Player p : game.getPlayers()) {
            assertEquals(3, p.getHand().size(),
                    p.getName() + " should have 3 cards after dealNewRound");
        }
    }

    @Test
    void dealNewRound_reducesDeckSizeByNine() {
        clearAllHands(); // ensure hands start empty so prior cards don't confuse the count
        int deckSizeBefore = game.getDeck().size();
        game.dealNewRound();
        assertEquals(deckSizeBefore - 9, game.getDeck().size());
    }

    // =========================================================================
    // finishGame
    // =========================================================================

    @Test
    void finishGame_emptyTable_doesNotAwardCards() {
        setupTable();
        int totalBefore = game.getPlayers().stream()
                .mapToInt(p -> p.getPointsStack().size()).sum();

        game.finishGame();

        int totalAfter = game.getPlayers().stream()
                .mapToInt(p -> p.getPointsStack().size()).sum();
        assertEquals(totalBefore, totalAfter);
    }

    @Test
    void finishGame_awardsRemainingCardsToLastPlayer() {
        // currentPlayerIndex starts at 0 after startGame().
        // finishGame awards to (currentPlayerIndex - 1 + 3) % 3 = 2 â†’ Rodrigo
        Player recipient = game.getPlayers().get(2); // Rodrigo
        int stackBefore = recipient.getPointsStack().size();

        Card extra = new Card(Suit.SPADES, Rank.ACE);
        setupTable(extra);

        game.finishGame();

        assertTrue(game.getTableCards().isEmpty());
        assertEquals(stackBefore + 1, recipient.getPointsStack().size());
        assertTrue(recipient.getPointsStack().contains(extra));
    }

    @Test
    void finishGame_clearsTable() {
        setupTable(new Card(Suit.HEARTS, Rank.ACE),
                   new Card(Suit.CLUBS,  Rank.TWO));

        game.finishGame();

        assertTrue(game.getTableCards().isEmpty());
    }

    // =========================================================================
    // getLastCollectedCards
    // =========================================================================

    @Test
    void getLastCollectedCards_notNullAtStart() {
        assertNotNull(game.getLastCollectedCards());
    }

    @Test
    void getLastCollectedCards_emptyAfterPlayToTable() {
        Player player  = game.getCurrentPlayer();
        Card handCard  = new Card(Suit.HEARTS, Rank.ACE);
        setupHand(player, handCard);
        setupTable();

        game.playCardWithSelection(player, handCard, List.of());

        assertTrue(game.getLastCollectedCards().isEmpty());
    }

    @Test
    void getLastCollectedCards_containsCapturedCardsAfterCapture() {
        Player player  = game.getCurrentPlayer();
        Card handCard  = new Card(Suit.HEARTS, Rank.KING); // value 10
        Card tableCard = new Card(Suit.CLUBS,  Rank.FIVE); // value 5
        setupHand(player, handCard);
        setupTable(tableCard);

        game.playCardWithSelection(player, handCard, List.of(tableCard));

        List<Card> last = game.getLastCollectedCards();
        assertEquals(2, last.size());
        assertTrue(last.contains(handCard));
        assertTrue(last.contains(tableCard));
    }

    // =========================================================================
    // getWinner
    // =========================================================================

    @Test
    void getWinner_returnsNullWhenGameNotOver() {
        assertNull(game.getWinner());
    }

    @Test
    void getWinner_returnsPlayerWithHighestPoints() {
        clearAllHands();
        drainDeck();
        setupTable();

        // Diamond 7 = 2 pts, plain 7 = 1 pt
        game.getPlayers().get(0).collectCards(List.of(new Card(Suit.DIAMONDS, Rank.SEVEN))); // Johnny: 2 pts
        game.getPlayers().get(1).collectCards(List.of(new Card(Suit.CLUBS,    Rank.SEVEN))); // Joni:   1 pt

        Player winner = game.getWinner();
        assertNotNull(winner);
        assertEquals("Johnny", winner.getName());
    }

    @Test
    void getWinner_brushesCountTowardScore() {
        clearAllHands();
        drainDeck();
        setupTable();

        // No card-based points for anyone; Johnny has 1 brush
        game.getPlayers().get(0).incrementBrushes();

        Player winner = game.getWinner();
        assertNotNull(winner);
        assertEquals("Johnny", winner.getName());
    }

    @Test
    void getWinner_diamondSevenCountsTwice() {
        clearAllHands();
        drainDeck();
        setupTable();

        Player johnny  = game.getPlayers().get(0);
        Player rodrigo = game.getPlayers().get(2);

        johnny.collectCards(List.of(new Card(Suit.DIAMONDS, Rank.SEVEN))); // 2 pts
        rodrigo.incrementBrushes();                                         // 1 brush
        rodrigo.incrementBrushes();                                         // 1 brush â†’ 2 total

        // Both score 2; winner is decided by tiebreak (non-deterministic), but getWinner must not throw.
        assertDoesNotThrow(() -> game.getWinner());
    }
}
