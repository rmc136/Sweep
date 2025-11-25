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

public class HomeScreenUI extends ScreenAdapter {
    private final Game game;
    private Stage stage;
    private Skin skin;
    private static final float VIRTUAL_WIDTH = 1280;
    private static final float VIRTUAL_HEIGHT = 720;

    public HomeScreenUI(Game game) {
        this.game = game;
        stage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        skin = new Skin(Gdx.files.internal("uiskin.json"));


        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Logo + title row
        Table titleRow = new Table();
        Image logo = new Image(new Texture("icon.png"));

        titleRow.add(logo).padRight(10).size(64,64);
        titleRow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Brush", skin));

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
                game.setScreen((Screen) new SingleplayerModesUI(game)); // 🚀 start the game
                return true;
            }
            return false;
        });

        // For now, other buttons just print
        multiBtn.addListener(event -> {
            if (multiBtn.isPressed()) {
                System.out.println("Multiplayer not implemented yet!");
                return true;
            }
            return false;
        });

        rulesBtn.addListener(event -> {
            if (rulesBtn.isPressed()) {
                game.setScreen((Screen) new RulesScreenUI(game));
                return true;
            }
            return false;
        });
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

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
}
