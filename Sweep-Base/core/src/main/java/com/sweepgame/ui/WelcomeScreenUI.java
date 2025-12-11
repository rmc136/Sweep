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
import com.sweepgame.utils.FontManager;
import com.sweepgame.utils.LayoutHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WelcomeScreenUI extends ScreenAdapter {
    private static final Logger logger = LoggerFactory.getLogger(WelcomeScreenUI.class);
    
    private final SweepGame game;
    private Stage stage;
    
    public WelcomeScreenUI(SweepGame game) {
        this.game = game;
        logger.info("WelcomeScreenUI initialized");
    }
    
    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);
        
        // Logo
        try {
            Texture logoTexture = new Texture(Gdx.files.internal("icon.png"));
            Image logo = new Image(logoTexture);
            float logoSize = LayoutHelper.getInstance().scale(300);
            logo.setSize(logoSize, logoSize);
            root.add(logo).size(logoSize).padBottom(LayoutHelper.getInstance().scale(60)).row();
            logger.debug("Logo loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load logo", e);
            // Fallback: show text title
            BitmapFont titleFont = FontManager.getInstance().getLargeFont();
            Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
            Label titleLabel = new Label("SWEEP", titleStyle);
            root.add(titleLabel).padBottom(LayoutHelper.getInstance().scale(60)).row();
        }
        
        // Title (below logo)
        BitmapFont titleFont = FontManager.getInstance().getLargeFont();
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
        Label titleLabel = new Label("SWEEP", titleStyle);
        root.add(titleLabel).padBottom(LayoutHelper.getInstance().scale(80)).row();
        
        // Login Button
        TextButton loginButton = createButton("LOGIN");
        loginButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                logger.info("Login button clicked");
                game.setScreen(new LoginScreenUI(game));
            }
        });
        root.add(loginButton).width(LayoutHelper.getInstance().scale(400)).height(LayoutHelper.getInstance().scale(100))
            .padBottom(LayoutHelper.getInstance().scale(30)).row();
        
        // Register Button
        TextButton registerButton = createButton("REGISTER");
        registerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                logger.info("Register button clicked");
                game.setScreen(new RegisterScreenUI(game));
            }
        });
        root.add(registerButton).width(LayoutHelper.getInstance().scale(400)).height(LayoutHelper.getInstance().scale(100))
            .padBottom(LayoutHelper.getInstance().scale(40)).row();
        
        // Skip button (for testing without login)
        BitmapFont skipFont = FontManager.getInstance().getSmallFont();
        TextButton.TextButtonStyle skipStyle = new TextButton.TextButtonStyle();
        skipStyle.font = skipFont;
        skipStyle.fontColor = Color.LIGHT_GRAY;
        skipStyle.downFontColor = Color.GRAY;
        
        TextButton skipButton = new TextButton("Skip (Play Offline)", skipStyle);
        skipButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                logger.info("Skip button clicked - going to home screen");
                game.setScreen(new HomeScreenUI(game));
            }
        });
        root.add(skipButton).width(LayoutHelper.getInstance().scale(300)).height(LayoutHelper.getInstance().scale(60));
        
        logger.info("WelcomeScreenUI screen shown");
    }
    
    private TextButton createButton(String text) {
        BitmapFont font = FontManager.getInstance().getDefaultFont();
        
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.downFontColor = Color.GRAY;
        
        return new TextButton(text, style);
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
        logger.info("WelcomeScreenUI disposed");
    }
}
