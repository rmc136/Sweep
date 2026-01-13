package com.sweepgame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sweepgame.SweepGame;
import com.sweepgame.game.MultiplayerMode;
import com.sweepgame.game.SweepGameUI;
import com.sweepgame.network.AuthService;
import com.sweepgame.network.GameStateDTO;
import com.sweepgame.network.WebSocketManager;
import com.sweepgame.utils.FontManager;
import com.sweepgame.utils.LayoutHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatchmakingQueueUI extends ScreenAdapter implements WebSocketManager.GameStateListener {
    private static final Logger logger = LoggerFactory.getLogger(MatchmakingQueueUI.class);

    private final SweepGame game;
    private final WebSocketManager wsManager;
    private final boolean isRanked;
    private final String myUsername;
    private Stage stage;
    private Skin skin;

    private Label statusLabel;
    private Label queueSizeLabel;
    private Label spinnerLabel;
    private float spinnerRotation = 0;

    private final AuthService authService;
    private final LayoutHelper layoutHelper;

    public MatchmakingQueueUI(SweepGame game, WebSocketManager wsManager, String accessToken, boolean isRanked) {
        this.game = game;
        this.wsManager = wsManager;
        this.isRanked = isRanked;
        this.myUsername = Gdx.app.getPreferences("SweepAuth").getString("username", "Player");
        this.authService = game.getAuthService();
        this.layoutHelper = LayoutHelper.getInstance();

        logger.info("MatchmakingQueueUI initialized (ranked: {})", isRanked);

        // Connect and join queue
        wsManager.setListener(this);
        connectAndJoin(accessToken);
    }

    private void connectAndJoin(String token) {
        // Just connect here, waiting for onConnect() callback to join
        wsManager.connect(token);
    }

    @Override
    public void onConnect() {
        logger.info("Connected to WebSocket, now joining queue...");
        wsManager.joinQueue(isRanked);
    }

    @Override
    public void show() {
        try {
            float width = layoutHelper.getViewportWidth();
            float height = layoutHelper.getViewportHeight();

            stage = new Stage(new FitViewport(width, height));
            skin = new Skin(Gdx.files.internal("uiskin.json"));

            FontManager fontManager = FontManager.getInstance();
            skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class);
            skin.add("default-font", fontManager.getLargeFont(), com.badlogic.gdx.graphics.g2d.BitmapFont.class);

            Gdx.input.setInputProcessor(stage);

            Table root = new Table();
            root.setFillParent(true);
            stage.addActor(root);

            // Title
            Label titleLabel = new Label("FINDING MATCH...", skin);
            root.add(titleLabel).padBottom(layoutHelper.scale(50)).row();

            // Status container (just text now for cleaner look)
            Table statusBox = new Table();

            // Spinner
            spinnerLabel = new Label("⟳", skin);
            spinnerLabel.setColor(Color.CYAN);
            spinnerLabel.setOrigin(com.badlogic.gdx.utils.Align.center);
            statusBox.add(spinnerLabel).padBottom(layoutHelper.scale(20)).row();

            // Queue size label
            queueSizeLabel = new Label("Connecting...", skin);
            statusBox.add(queueSizeLabel).padBottom(layoutHelper.scale(15)).row();

            // Status label
            statusLabel = new Label("Please wait", skin);
            statusLabel.setColor(Color.LIGHT_GRAY);
            statusBox.add(statusLabel).padBottom(layoutHelper.scale(30));

            root.add(statusBox).padBottom(layoutHelper.scale(40)).row();

            // Cancel Button
            TextButton cancelButton = new TextButton("CANCEL", skin);
            cancelButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    logger.info("Cancelling matchmaking");
                    cancelQueue();
                }
            });
            root.add(cancelButton).width(layoutHelper.scale(200)).height(layoutHelper.scale(50));

            logger.info("MatchmakingQueueUI screen shown");
        } catch (Exception e) {
            logger.error("Error showing matchmaking screen", e);
        }
    }

    private void cancelQueue() {
        if (wsManager.isConnected()) {
            wsManager.leaveQueue();
        }
        wsManager.disconnect();
        game.setScreen(new MultiplayerMenuUI(game));
    }

    @Override
    public void onQueueUpdate(int queueSize) {
        logger.info("Queue update: {} players", queueSize);
        Gdx.app.postRunnable(() -> {
            if (queueSizeLabel != null) {
                queueSizeLabel.setText("Players in queue: " + queueSize);

                int playersNeeded = 3 - queueSize;
                if (playersNeeded > 0) {
                    statusLabel.setText(
                            "Waiting for " + playersNeeded + " more player" + (playersNeeded > 1 ? "s" : "") + "...");
                } else {
                    statusLabel.setText("Match starting...");
                }
            }
        });
    }

    @Override
    public void onMatchFound(String sessionId) {
        logger.info("Match found! Session: {}", sessionId);
        Gdx.app.postRunnable(() -> {
            MultiplayerMode multiplayerMode = new MultiplayerMode(wsManager, sessionId, myUsername);
            game.setScreen(new SweepGameUI(game, multiplayerMode, isRanked ? "ranked" : "casual"));
        });
    }

    @Override
    public void onGameStateUpdate(GameStateDTO state) {
        logger.info("Received game state update in queue UI - Game Starting!");
        // If we get a game state, it means the match has started.
        // We can use the session ID from the state to start the game.
        if (state.getSessionId() != null) {
            Gdx.app.postRunnable(() -> {
                MultiplayerMode multiMode = new MultiplayerMode(wsManager, state.getSessionId(), myUsername);
                multiMode.updateState(state); // Initialize players immediately

                game.setScreen(new SweepGameUI(game, multiMode, isRanked ? "ranked" : "casual"));
            });
        }
    }

    @Override
    public void onError(String message) {
        logger.error("Matchmaking error: {}", message);
        Gdx.app.postRunnable(() -> {
            if (statusLabel != null) {
                statusLabel.setText("Error: " + message);
                statusLabel.setColor(Color.RED);
            }
        });
    }

    @Override
    public void onDisconnect() {
        logger.warn("Disconnected from server");
        Gdx.app.postRunnable(() -> {
            cancelQueue();
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.4f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Animate spinner
        if (spinnerLabel != null) {
            spinnerRotation -= delta * 180;
            // Label rotation might need origin set correctly which we did.
            // Note: rotation on Label depends on if font supports it or if we rotate the
            // actor.
            // Rotating actor usually works.
            spinnerLabel.setRotation(spinnerRotation);
        }

        if (stage != null) {
            stage.act(delta);
            stage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
        if (skin != null) {
            skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class);
            skin.dispose();
        }
        logger.info("MatchmakingQueueUI disposed");
    }
}
