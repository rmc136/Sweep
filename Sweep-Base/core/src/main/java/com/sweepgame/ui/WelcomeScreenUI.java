package com.sweepgame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sweepgame.SweepGame;
import com.sweepgame.utils.FontManager;
import com.sweepgame.utils.LayoutHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WelcomeScreenUI extends ScreenAdapter {
    private static final Logger logger = LoggerFactory.getLogger(WelcomeScreenUI.class);

    private final SweepGame game;
    private Stage stage;
    private Skin skin;
    private final LayoutHelper layoutHelper;

    public WelcomeScreenUI(SweepGame game) {
        this.game = game;
        this.layoutHelper = LayoutHelper.getInstance();
        logger.info("WelcomeScreenUI initialized");
    }

    @Override
    public void show() {
        try {
            float width = layoutHelper.getViewportWidth();
            float height = layoutHelper.getViewportHeight();

            stage = new Stage(new FitViewport(width, height));
            skin = new Skin(Gdx.files.internal("uiskin.json"));

            // Replace bitmap fonts with crisp TTF fonts
            FontManager fontManager = FontManager.getInstance();
            skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class);
            skin.add("default-font", fontManager.getLargeFont(), com.badlogic.gdx.graphics.g2d.BitmapFont.class);

            Gdx.input.setInputProcessor(stage);

            Table root = new Table();
            root.setFillParent(true);
            stage.addActor(root);

            // Logo
            try {
                Texture logoTexture = new Texture(Gdx.files.internal("icon.png"));
                Image logo = new Image(logoTexture);
                float logoSize = layoutHelper.scale(150);
                root.add(logo).size(logoSize, logoSize).padBottom(layoutHelper.scale(30)).row();
                logger.debug("Logo loaded successfully");
            } catch (Exception e) {
                logger.error("Failed to load logo", e);
                Label titleLabel = new Label("SWEEP", skin);
                root.add(titleLabel).padBottom(layoutHelper.scale(30)).row();
            }

            // Title (below logo)
            Label titleLabel = new Label("SWEEP", skin);
            root.add(titleLabel).padBottom(layoutHelper.scale(40)).row();

            // Login Button
            TextButton loginButton = new TextButton("LOGIN", skin);
            loginButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    logger.info("Login button clicked");
                    game.setScreen(new LoginScreenUI(game));
                }
            });
            root.add(loginButton).width(layoutHelper.scale(300)).height(layoutHelper.scale(60))
                    .padBottom(layoutHelper.scale(15)).row();

            // Register Button
            TextButton registerButton = new TextButton("REGISTER", skin);
            registerButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    logger.info("Register button clicked");
                    game.setScreen(new RegisterScreenUI(game));
                }
            });
            root.add(registerButton).width(layoutHelper.scale(300)).height(layoutHelper.scale(60))
                    .padBottom(layoutHelper.scale(20)).row();

            // Skip button (Play Offline)
            TextButton skipButton = new TextButton("Skip (Play Offline)", skin);
            skipButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    logger.info("Skip button clicked - going to home screen");
                    game.setScreen(new HomeScreenUI(game));
                }
            });
            root.add(skipButton).width(layoutHelper.scale(200)).height(layoutHelper.scale(50));

            logger.info("WelcomeScreenUI screen shown");
        } catch (Exception e) {
            logger.error("Error showing welcome screen", e);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.4f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
        logger.info("WelcomeScreenUI disposed");
    }
}
