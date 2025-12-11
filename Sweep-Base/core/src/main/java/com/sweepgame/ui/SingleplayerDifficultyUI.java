package com.sweepgame.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sweepgame.game.SweepGameUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleplayerDifficultyUI extends ScreenAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SingleplayerDifficultyUI.class);

    private final Game game;
    private Stage stage;
    private Skin skin;
    private final String tournamentMode;

    public SingleplayerDifficultyUI(Game game, String tournamentMode) {
        logger.info("Initializing difficulty selection screen: tournament={}", tournamentMode);
        this.game = game;
        this.tournamentMode = tournamentMode;

        float width = 1280;
        float height = 720;
        
        // If on Android/iOS, use smaller viewport to make UI elements bigger
        if (Gdx.app.getType() == Application.ApplicationType.Android || Gdx.app.getType() == Application.ApplicationType.iOS) {
            width = 360;
            height = 640;
        }

        stage = new Stage(new FitViewport(width, height));
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskin.json"));
        
        // Replace bitmap fonts with crisp TTF fonts
        com.sweepgame.utils.FontManager fontManager = com.sweepgame.utils.FontManager.getInstance();
        skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class);
        skin.add("default-font", fontManager.getLargeFont(), com.badlogic.gdx.graphics.g2d.BitmapFont.class);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Title
        Label title = new Label("Choose Difficulty", skin);
        // No font scaling needed - TTF fonts are crisp at any size
        root.top().add(title).padTop(10).row();

        // Buttons for each mode
        TextButton easyBtn = new TextButton("Easy", skin);
        TextButton mediumBtn = new TextButton("Medium", skin);
        TextButton hardBtn = new TextButton("Hard", skin);
        TextButton pedradoBtn = new TextButton("Pedrado", skin);
        TextButton backBtn = new TextButton("Back", skin);
        TextButton homeBtn = new TextButton("Home", skin);

        // Add buttons to table
        root.add(easyBtn).pad(10).row();
        root.add(mediumBtn).pad(10).row();
        root.add(hardBtn).pad(10).row();
        root.add(pedradoBtn).pad(10).row();
        root.add().expandY(); // Spacer to push buttons to bottom
        root.row();
        root.add(backBtn).pad(10).bottom().center().row();     
        root.add(homeBtn).pad(10).bottom().center();

        // Button listeners
        easyBtn.addListener(event -> {
            if (easyBtn.isPressed()) {
                startGameWithMode("Easy");
                return true;
            }
            return false;
        });

        mediumBtn.addListener(event -> {
            if (mediumBtn.isPressed()) {
                startGameWithMode("Medium");
                return true;
            }
            return false;
        });

        hardBtn.addListener(event -> {
            if (hardBtn.isPressed()) {
                startGameWithMode("Hard");
                return true;
            }
            return false;
        });

        pedradoBtn.addListener(event -> {
            if (pedradoBtn.isPressed()) {
                startGameWithMode("Pedrado");
                return true;
            }
            return false;
        });

        backBtn.addListener(event -> {
            if (backBtn.isPressed()) {
                returnToModeSelection();
                return true;
            }
            return false;
        });

        homeBtn.addListener(event -> {
            if (homeBtn.isPressed()) {
                returnHome();
                return true;
            }
            return false;
        });
        
        logger.debug("Difficulty selection screen initialized successfully");
    }

    private void returnHome() {
        logger.info("Returning to home screen");
        game.setScreen(new HomeScreenUI(game));
    }

    private void returnToModeSelection() {
        logger.info("Returning to mode selection");
        game.setScreen(new SingleplayerModeSelectionUI(game));
    }

    private void startGameWithMode(String mode) {
        logger.info("Starting game: difficulty={}, tournament={}", mode, tournamentMode);
        // Pass the difficulty mode and tournament mode to SweepGameUI
        game.setScreen(new SweepGameUI(game, mode, tournamentMode));
        dispose(); // clean up current stage
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
        logger.debug("Disposing difficulty selection screen resources");
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }
}
