package com.sweepgame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.sweepgame.SweepGame;
import com.sweepgame.network.WebSocketManager;
import com.sweepgame.utils.FontManager;
import com.sweepgame.utils.LayoutHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiplayerMenuUI extends ScreenAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MultiplayerMenuUI.class);
    
    private final SweepGame game;
    private Stage stage;
    private WebSocketManager wsManager;
    private String accessToken;
    
    public MultiplayerMenuUI(SweepGame game) {
        this.game = game;
        this.wsManager = new WebSocketManager();
        
        // Get access token from preferences
        this.accessToken = Gdx.app.getPreferences("SweepAuth").getString("accessToken", null);
        
        logger.info("MultiplayerMenuUI initialized");
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
        Label titleLabel = new Label("MULTIPLAYER", titleStyle);
        root.add(titleLabel).padBottom(LayoutHelper.getInstance().scale(80)).row();
        
        // Casual Match Button
        TextButton casualButton = createModeButton(
            "CASUAL MATCH",
            "Play for fun, no stakes",
            false
        );
        root.add(casualButton).width(LayoutHelper.getInstance().scale(500)).height(LayoutHelper.getInstance().scale(120))
            .padBottom(LayoutHelper.getInstance().scale(40)).row();
        
        // Ranked Match Button
        TextButton rankedButton = createModeButton(
            "RANKED MATCH",
            "Compete for points",
            true
        );
        root.add(rankedButton).width(LayoutHelper.getInstance().scale(500)).height(LayoutHelper.getInstance().scale(120))
            .padBottom(LayoutHelper.getInstance().scale(60)).row();
        
        // Back Button
        TextButton backButton = createBackButton();
        root.add(backButton).width(LayoutHelper.getInstance().scale(200)).height(LayoutHelper.getInstance().scale(60));
        
        logger.info("MultiplayerMenuUI screen shown");
    }
    
    private TextButton createModeButton(String title, String subtitle, boolean isRanked) {
        BitmapFont titleFont = FontManager.getInstance().getDefaultFont();
        BitmapFont subtitleFont = FontManager.getInstance().getSmallFont();
        
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = titleFont;
        style.fontColor = Color.WHITE;
        style.downFontColor = Color.GRAY;
        
        TextButton button = new TextButton(title + "\n" + subtitle, style);
        button.getLabel().setWrap(true);
        button.getLabel().setAlignment(com.badlogic.gdx.utils.Align.center);
        
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                logger.info("Selected {} mode", isRanked ? "ranked" : "casual");
                joinQueue(isRanked);
            }
        });
        
        return button;
    }
    
    private TextButton createBackButton() {
        BitmapFont font = FontManager.getInstance().getDefaultFont();
        
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.downFontColor = Color.GRAY;
        
        TextButton button = new TextButton("BACK", style);
        
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                logger.info("Returning to home screen");
                game.setScreen(new HomeScreenUI(game));
            }
        });
        
        return button;
    }
    
    private void joinQueue(boolean isRanked) {
        // Mock token check for development
        if (accessToken == null || accessToken.isEmpty()) {
            // For now, allow even without token for testing UI
            logger.warn("No access token found, proceeding anyway (Mock Mode)");
        }
        
        logger.info("Joining {} queue", isRanked ? "ranked" : "casual");
        game.setScreen(new MatchmakingQueueUI(game, wsManager, accessToken, isRanked));
    }
    
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
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
        logger.info("MultiplayerMenuUI disposed");
    }
}
