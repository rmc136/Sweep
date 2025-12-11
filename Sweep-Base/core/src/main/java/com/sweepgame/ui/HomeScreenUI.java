package com.sweepgame.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.Texture;
import com.sweepgame.game.SweepGameUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.Application;

public class HomeScreenUI extends ScreenAdapter {
    private static final Logger logger = LoggerFactory.getLogger(HomeScreenUI.class);
    
    private final Game game;
    private Stage stage;
    private Skin skin;
    private static final float VIRTUAL_WIDTH = 1280;
    private static final float VIRTUAL_HEIGHT = 720;

    public HomeScreenUI(Game game) {
        logger.info("Initializing home screen");
        this.game = game;
        
        float width = VIRTUAL_WIDTH;
        float height = VIRTUAL_HEIGHT;
        
        // If on Android/iOS, use smaller viewport to make UI elements bigger
        if (Gdx.app.getType() == Application.ApplicationType.Android || Gdx.app.getType() == Application.ApplicationType.iOS) {
            width = 360;
            height = 640;
            logger.debug("Mobile platform detected, using viewport: {}x{}", width, height);
        }
        
        try {
            stage = new Stage(new FitViewport(width, height));
            skin = new Skin(Gdx.files.internal("uiskin.json"));
        
        // Replace bitmap fonts with crisp TTF fonts
        com.sweepgame.utils.FontManager fontManager = com.sweepgame.utils.FontManager.getInstance();
        skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class);
        skin.add("default-font", fontManager.getLargeFont(), com.badlogic.gdx.graphics.g2d.BitmapFont.class);


        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Logo + title row
        Table titleRow = new Table();
        Image logo = new Image(new Texture("icon.png"));
        logger.debug("Logo loaded successfully");

        titleRow.add(logo).padRight(10).size(64,64);
        titleRow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Sweep", skin));

        root.add(titleRow).padBottom(50).row();

        // Buttons
        TextButton singleBtn = new TextButton("Singleplayer", skin);
        TextButton multiBtn = new TextButton("Multiplayer", skin);
        TextButton rulesBtn = new TextButton("Rules", skin);

        root.add(singleBtn).pad(10).row();
        root.add(multiBtn).pad(10).row();
        root.add(rulesBtn).pad(10).row();

        // Button listeners
        singleBtn.addListener(event -> {
            if (singleBtn.isPressed()) {
                logger.info("Singleplayer button clicked");
                game.setScreen(new SingleplayerModeSelectionUI(game));
                return true;
            }
            return false;
        });

        // For now, other buttons just print
        multiBtn.addListener(event -> {
            if (multiBtn.isPressed()) {
                logger.info("Multiplayer button clicked (not implemented)");
                return true;
            }
            return false;
        });

        rulesBtn.addListener(event -> {
            if (rulesBtn.isPressed()) {
                logger.info("Rules button clicked");
                game.setScreen((Screen) new RulesScreenUI(game));
                return true;
            }
            return false;
        });
        
        logger.info("Home screen initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing home screen", e);
            throw e;
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.4f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        logger.debug("Disposing home screen resources");
        try {
            if (stage != null) stage.dispose();
            // Remove shared font from skin so it doesn't get disposed
            if (skin != null) {
                skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class);
                skin.dispose();
            }
        } catch (Exception e) {
            logger.error("Error disposing home screen resources", e);
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
}
