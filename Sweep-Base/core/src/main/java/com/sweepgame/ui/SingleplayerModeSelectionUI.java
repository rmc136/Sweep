package com.sweepgame.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class SingleplayerModeSelectionUI extends ScreenAdapter {

    private final Game game;
    private Stage stage;
    private Skin skin;

    public SingleplayerModeSelectionUI(Game game) {
        this.game = game;

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
        Label title = new Label("Singleplayer Modes", skin);
        root.top().add(title).padTop(10).row();

        // Buttons for each tournament mode
        TextButton singleGameBtn = new TextButton("Single Game", skin);
        TextButton firstTo4Btn = new TextButton("First to 4 Wins", skin);
        TextButton firstTo8Btn = new TextButton("First to 8 Wins", skin);
        TextButton backBtn = new TextButton("Back", skin);

        // Add buttons to table
        root.add(singleGameBtn).pad(10).row();
        root.add(firstTo4Btn).pad(10).row();
        root.add(firstTo8Btn).pad(10).row();
        root.row();
        root.add(backBtn).expandY().bottom().center();

        // Button listeners
        singleGameBtn.addListener(event -> {
            if (singleGameBtn.isPressed()) {
                selectTournamentMode("single");
                return true;
            }
            return false;
        });

        firstTo4Btn.addListener(event -> {
            if (firstTo4Btn.isPressed()) {
                selectTournamentMode("first_to_4");
                return true;
            }
            return false;
        });

        firstTo8Btn.addListener(event -> {
            if (firstTo8Btn.isPressed()) {
                selectTournamentMode("first_to_8");
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

    private void selectTournamentMode(String tournamentMode) {
        // Navigate to difficulty selection with the selected tournament mode
        game.setScreen(new SingleplayerDifficultyUI(game, tournamentMode));
        dispose();
    }

    private void returnHome() {
        game.setScreen(new HomeScreenUI(game));
        dispose();
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
