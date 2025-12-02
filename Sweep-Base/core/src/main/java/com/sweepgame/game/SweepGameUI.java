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

    public SweepGameUI(Game game, String mode) {
        this.game = game;
        this.mode = mode;
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
        gameLogic.startGame();

        humanPlayer = gameLogic.getPlayers().get(0);
        Player leftPlayer = gameLogic.getPlayers().get(1);
        Player rightPlayer = gameLogic.getPlayers().get(2);

        // Initialize modular UI
        // 1. Create ScoreUI first to generate labels
        scoreUI = new ScoreUI(skin, gameLogic.getPlayers());

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
        handUI = new HandUI(this, gameLogic.getPlayers().get(0), gameLogic, tableUI, this::refreshUI);
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

        seatUI.update();

        refreshUI();
    }

    private void refreshUI() {
        scoreUI.update(gameLogic.getPlayers());
        tableUI.update(gameLogic.getTableCards());
        handUI.update();

        // Let AI play if it's their turn
        while (!(gameLogic.getCurrentPlayer().equals(humanPlayer)) && !gameLogic.isGameOver()) {
            playAITurns();
            seatUI.update();
        }

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

    // Simple AI turn logic
    private void playAITurns() {
        for (int i = 1; i < gameLogic.getPlayers().size(); i++) {
            Player ai = gameLogic.getPlayers().get(i);
            if (!ai.getHand().isEmpty()) {
                Card cardToPlay = ai.getHand().get(0);
                List<Card> selectedForAI = new ArrayList<>();
                selectedForAI.add(cardToPlay);
                gameLogic.playCard(ai, cardToPlay, selectedForAI);
                List<Card> collected = new ArrayList<>(gameLogic.getLastCollectedCards());
                animateAICards(ai, cardToPlay, collected);
            }
        }
        refreshUI();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.4f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
        skin.dispose();
    }

    private void showWinner(String winnerName) {
        Player player = null;
        for (Player p : gameLogic.getPlayers()) {
            if (winnerName.equals(p.getName())) {
                player = p;
            }
        }
        int points = player.calculatePoints() + player.getBrushes();
        Label winnerLabel = new Label("Winner: " + winnerName + "! With " + points + " points!", skin);
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

        showReplayButton();
        showHomeButton();
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

    private void resetGame() {
        game.setScreen(new SweepGameUI(game, mode));
        // rebuild everything
    }

    private void returnHome() {
        game.setScreen(new HomeScreenUI(game));
    }
}
