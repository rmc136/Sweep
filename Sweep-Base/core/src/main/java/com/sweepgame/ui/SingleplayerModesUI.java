package com.sweepgame.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.sweepgame.game.SweepGameUI;

public class SingleplayerModesUI extends ScreenAdapter {

    private final Game game;
    private Stage stage;
    private Skin skin;

    public SingleplayerModesUI(Game game) {
        this.game = game;

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Title
        Label title = new Label("Singleplayer Modes", skin);
        title.setFontScale(2f);
        root.top().add(title).padTop(10).row();

        // Buttons for each mode
        TextButton easyBtn = new TextButton("Easy", skin);
        TextButton mediumBtn = new TextButton("Medium", skin);
        TextButton hardBtn = new TextButton("Hard", skin);
        TextButton pedradoBtn = new TextButton("Pedrado", skin);
        TextButton backBtn = new TextButton("Home", skin);

        // Add buttons to table
        root.add(easyBtn).pad(10).row();
        root.add(mediumBtn).pad(10).row();
        root.add(hardBtn).pad(10).row();
        root.add(pedradoBtn).pad(10).row();
        root.row();
        root.add(backBtn).expandY().bottom().center();

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
                returnHome();
                return true;
            }
            return false;
        });
    }

    private void returnHome() {
        game.setScreen(new HomeScreenUI(game));
    }

    private void startGameWithMode(String mode) {
        // Pass the mode to BrushGameUI if you want to handle difficulty inside the game
        game.setScreen(new SweepGameUI(game, mode));
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
        stage.dispose();
        skin.dispose();
    }
}
