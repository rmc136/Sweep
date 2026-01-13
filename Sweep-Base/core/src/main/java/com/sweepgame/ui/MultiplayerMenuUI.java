package com.sweepgame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
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
import com.sweepgame.network.WebSocketManager;
import com.sweepgame.utils.FontManager;
import com.sweepgame.utils.LayoutHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiplayerMenuUI extends ScreenAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MultiplayerMenuUI.class);

    private final SweepGame game;
    private Stage stage;
    private Skin skin;
    private WebSocketManager wsManager;
    private String accessToken;
    private final LayoutHelper layoutHelper;

    public MultiplayerMenuUI(SweepGame game) {
        this.game = game;
        this.wsManager = new WebSocketManager();
        this.layoutHelper = LayoutHelper.getInstance();

        // Get access token from preferences
        this.accessToken = Gdx.app.getPreferences("SweepAuth").getString("accessToken", null);

        logger.info("MultiplayerMenuUI initialized");
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
            Label titleLabel = new Label("MULTIPLAYER", skin);
            root.add(titleLabel).padBottom(layoutHelper.scale(50)).row();

            // Casual Match Button
            TextButton casualButton = new TextButton("CASUAL MATCH\nPlay for fun, no stakes", skin);
            casualButton.getLabel().setWrap(true);
            casualButton.getLabel().setAlignment(com.badlogic.gdx.utils.Align.center);
            casualButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    logger.info("Selected casual mode");
                    joinQueue(false);
                }
            });
            root.add(casualButton).width(layoutHelper.scale(400)).height(layoutHelper.scale(80))
                    .padBottom(layoutHelper.scale(20)).row();

            // Ranked Match Button
            TextButton rankedButton = new TextButton("RANKED MATCH\nCompete for points", skin);
            rankedButton.getLabel().setWrap(true);
            rankedButton.getLabel().setAlignment(com.badlogic.gdx.utils.Align.center);
            rankedButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    logger.info("Selected ranked mode");
                    joinQueue(true);
                }
            });
            root.add(rankedButton).width(layoutHelper.scale(400)).height(layoutHelper.scale(80))
                    .padBottom(layoutHelper.scale(40)).row();

            // Back Button
            TextButton backButton = new TextButton("BACK", skin);
            backButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    logger.info("Returning to home screen");
                    game.setScreen(new HomeScreenUI(game));
                }
            });
            root.add(backButton).width(layoutHelper.scale(200)).height(layoutHelper.scale(50));

            logger.info("MultiplayerMenuUI screen shown");
        } catch (Exception e) {
            logger.error("Error showing multiplayer menu", e);
        }
    }

    private void joinQueue(boolean isRanked) {
        if (accessToken == null || accessToken.isEmpty()) {
            logger.warn("No access token found, proceeding anyway (Mock Mode)");
        }

        logger.info("Joining {} queue", isRanked ? "ranked" : "casual");
        game.setScreen(new MatchmakingQueueUI(game, wsManager, accessToken, isRanked));
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
        logger.info("MultiplayerMenuUI disposed");
    }
}
