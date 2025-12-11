package com.sweepgame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.sweepgame.game.MultiplayerMode;
import com.sweepgame.SweepGame;
import com.sweepgame.game.SweepGameUI;
import com.sweepgame.network.GameStateDTO;
import com.sweepgame.network.WebSocketManager;
import com.sweepgame.utils.FontManager;
import com.sweepgame.utils.LayoutHelper;
import com.sweepgame.network.AuthService;
import com.sweepgame.ui.LoginScreenUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatchmakingQueueUI extends ScreenAdapter implements WebSocketManager.GameStateListener {
    private static final Logger logger = LoggerFactory.getLogger(MatchmakingQueueUI.class);
    
    private final SweepGame game;
    private final WebSocketManager wsManager;
    private final boolean isRanked;
    private final String myUsername;
    private Stage stage;
    
    private Label statusLabel;
    private Label queueSizeLabel;
    private boolean isRetrying = false;
    private final AuthService authService;
    private float spinnerRotation = 0;
    
    public MatchmakingQueueUI(SweepGame game, WebSocketManager wsManager, String accessToken, boolean isRanked) {
        this.game = game;
        this.wsManager = wsManager;
        this.isRanked = isRanked;
        this.myUsername = Gdx.app.getPreferences("SweepAuth").getString("username", "Player");
        this.authService = game.getAuthService();
        
        logger.info("MatchmakingQueueUI initialized (ranked: {})", isRanked);
        
        // Connect and join queue
        wsManager.setListener(this);
        connectAndJoin(accessToken);
    }
    
    private void connectAndJoin(String token) {
        wsManager.connect(token);
        wsManager.joinQueue(isRanked);
    }
    
    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);
        
        // Title
        BitmapFont titleFont = FontManager.getInstance().getLargeFont();
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
        Label titleLabel = new Label("FINDING MATCH...", titleStyle);
        root.add(titleLabel).padBottom(LayoutHelper.getInstance().scale(80)).row();
        
        // Status container
        Table statusBox = new Table();
        statusBox.setBackground(createBoxBackground());
        
        // Spinner (simple text for now, can be replaced with animation)
        BitmapFont spinnerFont = FontManager.getInstance().getLargeFont();
        Label.LabelStyle spinnerStyle = new Label.LabelStyle(spinnerFont, Color.CYAN);
        Label spinnerLabel = new Label("⟳", spinnerStyle);
        statusBox.add(spinnerLabel).padBottom(LayoutHelper.getInstance().scale(40)).row();
        
        // Queue size label
        BitmapFont queueFont = FontManager.getInstance().getDefaultFont();
        Label.LabelStyle queueStyle = new Label.LabelStyle(queueFont, Color.WHITE);
        queueSizeLabel = new Label("Connecting...", queueStyle);
        statusBox.add(queueSizeLabel).padBottom(LayoutHelper.getInstance().scale(20)).row();
        
        // Status label
        BitmapFont statusFont = FontManager.getInstance().getSmallFont();
        Label.LabelStyle statusStyle = new Label.LabelStyle(statusFont, Color.LIGHT_GRAY);
        statusLabel = new Label("Please wait", statusStyle);
        statusBox.add(statusLabel).padBottom(LayoutHelper.getInstance().scale(40));
        
        root.add(statusBox).width(LayoutHelper.getInstance().scale(700)).height(LayoutHelper.getInstance().scale(400))
            .padBottom(LayoutHelper.getInstance().scale(60)).row();
        
        // Cancel Button
        TextButton cancelButton = createCancelButton();
        root.add(cancelButton).width(LayoutHelper.getInstance().scale(300)).height(LayoutHelper.getInstance().scale(80));
        
        logger.info("MatchmakingQueueUI screen shown");
    }
    
    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createBoxBackground() {
        // Simple colored background
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(0.15f, 0.15f, 0.2f, 0.8f);
        pixmap.fill();
        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(texture));
    }
    
    private TextButton createCancelButton() {
        BitmapFont font = FontManager.getInstance().getDefaultFont();
        
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.downFontColor = Color.GRAY;
        
        TextButton button = new TextButton("CANCEL", style);
        
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                logger.info("Cancelling matchmaking");
                cancelQueue();
            }
        });
        
        return button;
    }
    
    private void cancelQueue() {
        wsManager.leaveQueue();
        wsManager.disconnect();
        game.setScreen(new MultiplayerMenuUI(game));
    }
    
    @Override
    public void onQueueUpdate(int queueSize) {
        logger.info("Queue update: {} players", queueSize);
        Gdx.app.postRunnable(() -> {
            queueSizeLabel.setText("Players in queue: " + queueSize);
            
            int playersNeeded = 3 - queueSize;
            if (playersNeeded > 0) {
                statusLabel.setText("Waiting for " + playersNeeded + " more player" + (playersNeeded > 1 ? "s" : "") + "...");
            } else {
                statusLabel.setText("Match starting...");
            }
        });
    }
    
    @Override
    public void onMatchFound(String sessionId) {
        logger.info("Match found! Session: {}", sessionId);
        Gdx.app.postRunnable(() -> {
            // Create multiplayer mode
            MultiplayerMode multiplayerMode = new MultiplayerMode(wsManager, sessionId, myUsername);
            
            // Create SweepGameUI with multiplayer mode
            game.setScreen(new SweepGameUI(game, multiplayerMode, isRanked ? "ranked" : "casual"));
        });
    }
    
    @Override
    public void onGameStateUpdate(GameStateDTO state) {
        // Not used in queue screen
    }
    
    @Override
    public void onError(String message) {
        logger.error("Matchmaking error: {}", message);
        Gdx.app.postRunnable(() -> {
            statusLabel.setText("Error: " + message);
            statusLabel.setColor(Color.RED);
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
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Animate spinner
        spinnerRotation += delta * 180; // Rotate 180 degrees per second
        
        stage.act(delta);
        stage.draw();
    }
    
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    
    @Override
    public void dispose() {
        stage.dispose();
        logger.info("MatchmakingQueueUI disposed");
    }
}
