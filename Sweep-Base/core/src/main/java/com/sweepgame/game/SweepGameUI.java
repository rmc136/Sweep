package com.sweepgame.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sweepgame.SweepGame;
import com.sweepgame.cards.Card;
import com.sweepgame.cards.Deck;
import com.sweepgame.cards.Player;
import com.sweepgame.ui.*;
import com.sweepgame.utils.LayoutHelper;
import java.util.ArrayList;
import java.util.List;

public class SweepGameUI implements Screen {

    private Stage stage;
    private Skin skin;

    private SweepLogic gameLogic;
    private Player humanPlayer;

    // Modular UI components
    private ScoreUI scoreUI;
    private TableUI tableUI;
    private HandUI handUI;
    private PlayerSeatUI seatUI;
    private final Game game;
    private final String mode;
    private final String tournamentMode;
    private TournamentManager tournamentManager;
    private DifficultyConfig difficultyConfig;
    private boolean aiTurnInProgress = false;
    private Label timerLabel;
    private com.badlogic.gdx.graphics.g2d.BitmapFont timerFont; // Keep reference to dispose
    private float timeRemaining;
    private boolean timerActive; // Track if AI is currently playing

    public SweepGameUI(Game game, String mode, String tournamentMode) {
        this.game = game;
        this.mode = mode;
        this.tournamentMode = tournamentMode;
        this.tournamentManager = TournamentManager.getInstance();
        this.difficultyConfig = new DifficultyConfig(mode);
        // Initialize tournament on first creation
        if (tournamentManager.getTournamentMode() == null || !tournamentManager.getTournamentMode().equals(tournamentMode)) {
            tournamentManager.initializeTournament(tournamentMode);
        }
    }
    @Override
    public void show() {
        LayoutHelper layout = LayoutHelper.getInstance();
        stage = new Stage(new FitViewport(layout.getViewportWidth(), layout.getViewportHeight()));
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskin.json"));
        
        // Replace bitmap fonts with crisp TTF fonts
        com.sweepgame.utils.FontManager fontManager = com.sweepgame.utils.FontManager.getInstance();
        skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class);
        skin.add("default-font", fontManager.getLargeFont(), com.badlogic.gdx.graphics.g2d.BitmapFont.class);

        // Initialize game logic
        gameLogic = new SweepLogic();
        int startingPlayer = tournamentManager.getStartingPlayerIndex();
        gameLogic.startGame(startingPlayer);

        // Initialize tournament manager with players
        tournamentManager.initializePlayers(gameLogic.getPlayers());

        humanPlayer = gameLogic.getPlayers().get(0);
        Player leftPlayer = gameLogic.getPlayers().get(1);
        Player rightPlayer = gameLogic.getPlayers().get(2);

        // Initialize modular UI
        // 1. Create ScoreUI first to generate labels
        scoreUI = new ScoreUI(skin, gameLogic.getPlayers(), tournamentManager);

        // 2. Initialize Player Seats (Opponents) with their score labels
        // Assuming player 0 is main player, 1 is left, 2 is right (or similar logic)
        // Let's check how players are passed.
        // In SweepGameUI, players list: 0=Main, 1=Left, 2=Right (usually)
        // Let's verify player indices.
        // PlayerSeatUI takes (left, right).
        // If 3 players: Main(0), Left(1), Right(2).
        
        // Pass labels for Left(1) and Right(2)
        // Pass labels for Left(1) and Right(2)
        seatUI = new PlayerSeatUI(skin, gameLogic.getPlayers().get(1), gameLogic.getPlayers().get(2), scoreUI.getLabel(1), scoreUI.getLabel(2));
        seatUI.setScoreLabels(scoreUI.getLabel(1), scoreUI.getLabel(2)); // Ensure they are stored
        stage.addActor(seatUI.getTable());

        // 3. Initialize Table UI
        tableUI = new TableUI();
        stage.addActor(tableUI.getTable());

        // 4. Initialize Hand UI (Main Player)
        handUI = new HandUI(this, gameLogic.getPlayers().get(0), gameLogic, tableUI, this::refreshUI, difficultyConfig);
        // stage.addActor(handUI.getTable()); // We add it to mainTable instead

        Table mainTable = new Table();
        mainTable.setFillParent(true);

        mainTable.center().add(tableUI.getTable()).expand().row();
        
        // Bottom Row: [Hand (Centered)]
        Table bottomRow = new Table();
        bottomRow.add(handUI.getTable()).expandX().center();
        mainTable.bottom().add(bottomRow).fillX();

        stage.addActor(mainTable);
        stage.addActor(seatUI.getTable());
        
        // Add User Score Label absolutely to the stage to ensure it never moves
        Label userScore = scoreUI.getLabel(0);
        userScore.setPosition(20, 20); // Fixed position at bottom-left
        stage.addActor(userScore);
        
        // Add Exit Game button in bottom right corner
        TextButton exitButton = new TextButton("Exit Game", skin);
        exitButton.setPosition(stage.getWidth() - exitButton.getWidth() - 20, 20);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                returnHome();
            }
        });
        stage.addActor(exitButton);
        
        // Add timer label if difficulty has timer
        if (difficultyConfig.hasTimer()) {
            Label.LabelStyle timerStyle = new Label.LabelStyle();
            timerFont = com.sweepgame.utils.FontManager.getInstance().createTimerFont();
            timerStyle.font = timerFont;
            timerStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
            timerLabel = new Label("", timerStyle);
            timerLabel.setPosition(stage.getWidth() / 2f - 50, stage.getHeight() - 50);
            stage.addActor(timerLabel);
            resetTimer();
        }

        seatUI.update();

        refreshUI();
    }

    public boolean isAITurnInProgress() {
        return aiTurnInProgress;
    }

    private void refreshUI() {
        scoreUI.update(gameLogic.getPlayers());
        tableUI.update(gameLogic.getTableCards());
        handUI.update();

        // Let AI play if it's their turn - start the sequence
        if (!(gameLogic.getCurrentPlayer().equals(humanPlayer)) && !gameLogic.isGameOver()) {
            aiTurnInProgress = true;
            pauseTimer(); 
            scheduleNextAITurn();
            checkGameState();
            System.out.println("HERE: " + gameLogic.getCurrentPlayer().getName());
        } else {
            // Check for new round or game over only when it's human's turn
            aiTurnInProgress = false;
            checkGameState();
            // Only reset timer if it's actually the human's turn and game is not over
            if (gameLogic.getCurrentPlayer().equals(humanPlayer) && !gameLogic.isGameOver()) {
                resetTimer();
            }
        }
    }

    private void checkGameState() {

        if (gameLogic.getCurrentPlayer().equals(humanPlayer)) {
            aiTurnInProgress = false; // Re-enable user input
        }
        System.out.println("HERE: " + gameLogic.allHandsEmpty() + " " + !gameLogic.getDeck().isEmpty());
        // If all players are out of cards but deck still has cards → deal new ones
        if (gameLogic.allHandsEmpty() && !gameLogic.getDeck().isEmpty()) {
            gameLogic.dealNewRound();
            handUI.update();
        }

        if (gameLogic.isGameOver()) {
            gameLogic.finishGame();
            tableUI.update(gameLogic.getTableCards());
            Player winner = gameLogic.getWinner();
            if (winner != null) {
                scoreUI.update(gameLogic.getPlayers());
                showWinner(winner.getName());
            }
        }
    }

    // Schedule the next AI turn with a delay
    private void scheduleNextAITurn() {
        // Check if current player is still an AI (not human)
        if (gameLogic.getCurrentPlayer().equals(humanPlayer) || gameLogic.isGameOver()) {
            resetTimer();
            seatUI.update();
            checkGameState();
            return;
        }

        // Schedule this AI player's turn after a delay
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                playOneAITurn();
            }
        }, 2.5f); // 2.5 second delay
    }

    // Play a single AI turn
    private void playOneAITurn() {
        // Get the current player from game logic (respects turn order)
        Player currentPlayer = gameLogic.getCurrentPlayer();
        
        // Safety check: make sure it's not the human player
        if (currentPlayer.equals(humanPlayer)) {
            checkGameState();
            return;
        }

        if (!currentPlayer.getHand().isEmpty()) {
            Card cardToPlay = currentPlayer.getHand().get(0);
            List<Card> selectedForAI = new ArrayList<>();
            selectedForAI.add(cardToPlay);
            gameLogic.playCard(currentPlayer, cardToPlay, selectedForAI);
            List<Card> collected = new ArrayList<>(gameLogic.getLastCollectedCards());
            animateAICards(currentPlayer, cardToPlay, collected);
        }

        // Update UI after this AI's turn
        scoreUI.update(gameLogic.getPlayers());
        tableUI.update(gameLogic.getTableCards());
        seatUI.update();

        // Schedule the next AI turn (game logic already advanced the turn)
        scheduleNextAITurn();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.4f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Update timer if active
        if (timerActive && difficultyConfig.hasTimer()) {
            timeRemaining -= delta;
            updateTimerDisplay();
            
            if (timeRemaining <= 0) {
                handleTimerExpiration();
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        stage.dispose();
        // Remove shared font from skin so it doesn't get disposed
        skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class);
        skin.dispose();
        if (timerFont != null) {
            timerFont.dispose();
        }
    }
    
    private void resetTimer() {
        if (difficultyConfig.hasTimer() && !gameLogic.isGameOver()) {
            timeRemaining = difficultyConfig.getTimerSeconds();
            timerActive = true;
            updateTimerDisplay();
        }
    }
    
    private void pauseTimer() {
        timerActive = false;
    }
    
    private void updateTimerDisplay() {
        if (timerLabel != null) {
            int seconds = (int) Math.ceil(timeRemaining);
            timerLabel.setText("Time: " + seconds + "s");
        }
    }
    
    private void handleTimerExpiration() {
        timerActive = false;
        
        // Auto-play first card from hand when timer expires
        if (!humanPlayer.getHand().isEmpty()) {
            Card firstCard = humanPlayer.getHand().get(0);
            List<Card> emptySelection = new ArrayList<>();
            gameLogic.playCardWithSelection(humanPlayer, firstCard, emptySelection);
            
            // Update UI manually since we're not going through HandUI
            scoreUI.update(gameLogic.getPlayers());
            tableUI.update(gameLogic.getTableCards());
            handUI.update();
            
            // Now check if it's AI's turn
            if (!(gameLogic.getCurrentPlayer().equals(humanPlayer)) && !gameLogic.isGameOver()) {
                aiTurnInProgress = true;
                pauseTimer();
                scheduleNextAITurn();
                
            } else {
                // Still human's turn (shouldn't happen) or game over
                checkGameState();
                if (gameLogic.getCurrentPlayer().equals(humanPlayer) && !gameLogic.isGameOver()) {
                    resetTimer();
                }
            }
        }
    }

    private void showWinner(String winnerName) {
        Player player = null;
        for (Player p : gameLogic.getPlayers()) {
            if (winnerName.equals(p.getName())) {
                player = p;
            }
        }
        
        // Record the win in tournament manager
        tournamentManager.recordWin(winnerName);
        
        int points = player.calculatePoints() + player.getBrushes();
        int gamesWon = tournamentManager.getWins(winnerName);
        
        // Check if tournament is complete
        boolean tournamentComplete = tournamentManager.isTournamentComplete();
        
        String message;
        if (tournamentComplete) {
            message = "Tournament Winner: " + winnerName + "!\nGames Won: " + gamesWon + "/" + tournamentManager.getWinsNeeded();
        } else {
            message = "Game Winner: " + winnerName + "! (" + points + " pts)\nGames Won: " + gamesWon + "/" + tournamentManager.getWinsNeeded();
        }
        
        Label winnerLabel = new Label(message, skin);
        winnerLabel.setFontScale(2f);

        winnerLabel.setPosition(stage.getWidth() / 2f - winnerLabel.getWidth() / 2f,
            stage.getHeight() / 2f);

        stage.addActor(winnerLabel);

        winnerLabel.getColor().a = 0;
        winnerLabel.addAction(
            Actions.sequence(
                Actions.fadeIn(1f),
                Actions.forever(
                    Actions.sequence(
                        Actions.scaleTo(1.2f, 1.2f, 0.5f),
                        Actions.scaleTo(1f, 1f, 0.5f)
                    )
                )
            )
        );

        // Show appropriate button based on tournament status
        if (tournamentComplete) {
            showReplayButton(); // Tournament is over, show replay
        } else {
            showNextGameButton(); // Tournament continues, show next game
        }
        
        showHomeButton();
    }

    private void showNextGameButton() {
        TextButton nextGameBtn = new TextButton("Next Game", skin);

        nextGameBtn.setPosition(stage.getWidth() / 2f - nextGameBtn.getWidth() / 2f,
            stage.getHeight() / 2f - 100);

        nextGameBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startNextGame();
            }
        });

        stage.addActor(nextGameBtn);
    }

    private void showReplayButton() {
        TextButton replayBtn = new TextButton("Replay", skin);

        replayBtn.setPosition(stage.getWidth() / 2f - replayBtn.getWidth() / 2f,
            stage.getHeight() / 2f - 100);

        replayBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                resetGame();
            }
        });

        stage.addActor(replayBtn);
    }

    private void showHomeButton() {
        TextButton homeBtn = new TextButton("Home", skin);

        homeBtn.setPosition(stage.getWidth() / 2f - homeBtn.getWidth() / 2f,
            stage.getHeight() / 2.2f - 100);

        homeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                returnHome();
            }
        });

        stage.addActor(homeBtn);
    }

    private void animateAICards(Player ai, Card aiCard, List<Card> collectedCards) {
        Stage stage = this.stage;
        if (stage == null) return;

        // AI hand position
        float aiX = (ai == gameLogic.getPlayers().get(1)) ? 100 : stage.getWidth() - 200;
        float aiY = stage.getHeight() - 120;

        // Table center
        Table tableParent = tableUI.getTable();
        float centerX = tableParent.getX() + tableParent.getWidth() / 2f;
        float centerY = tableParent.getY() + tableParent.getHeight() / 2f;

        // Build animation list: always include AI card, then collected table cards
        List<Card> animCards = new ArrayList<>();
        animCards.add(aiCard);
        for (Card c : collectedCards) {
            if (!c.equals(aiCard)) animCards.add(c);
        }

        int n = animCards.size();
        float spacing = 30f;

        for (int i = 0; i < n; i++) {
            Card card = animCards.get(i);
            Texture texture = new Texture("cards/" + card.getImageName());
            Image img = new Image(texture);

            LayoutHelper layout = LayoutHelper.getInstance();
            img.setSize(layout.getAnimCardWidth(), layout.getAnimCardHeight());
            img.setPosition(aiX, aiY);
            stage.addActor(img);

            float targetX = centerX - ((n - 1) * spacing) / 2f + i * spacing;
            float targetY = centerY;

            boolean moveToAIHand = !collectedCards.isEmpty(); // only move back if AI captured

            if (moveToAIHand) {
                img.addAction(Actions.sequence(
                    Actions.moveTo(targetX, targetY, 0.5f),
                    Actions.delay(1f),
                    Actions.moveTo(aiX, aiY, 0.5f),
                    Actions.run(img::remove)
                ));
            } else {
                img.addAction(Actions.sequence(
                    Actions.moveTo(targetX, targetY, 0.5f),
                    Actions.run(img::remove)
                ));
            }
        }
    }

    private void startNextGame() {
        // Start a new game while preserving tournament state
        // The new instance will use the singleton TournamentManager which has the updated state
        game.setScreen(new SweepGameUI(game, mode, tournamentMode));
    }

    private void resetGame() {
        // Restart the tournament
        tournamentManager.initializeTournament(tournamentMode);
        game.setScreen(new SweepGameUI(game, mode, tournamentMode));
    }

    private void returnHome() {
        game.setScreen(new HomeScreenUI(game));
    }
}
