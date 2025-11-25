package com.sweepgame.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class RulesScreenUI extends ScreenAdapter {

    private final Game game;
    private Stage stage;
    private Skin skin;

    public RulesScreenUI(Game game) {
        this.game = game;

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Title
        Label title = new Label("Brush Game Rules", skin);
        title.setFontScale(2f);
        root.top().add(title).padBottom(20).row();

        // Scrollable rules
        String rulesText =
            "BASIC RULES:\n"+ "\n"+
                "1. Each player takes turns playing a card.\n" +
                "2. Try to collect cards that sum up to 15.\n" +
                "3. You can only play one of the cards in your hand per turn, and try to combine it with the table cards to make 15 selecting the ones you want and prefer\n" +
                "4. If you capture all table cards + your played card, you get a brush bonus (+1 point in the end).\n" +
                "5. The player with most points at the end wins!\n" +
                "6. If no cards can be collected, your card stays on the table.\n" +
                "7. Continue until all cards in deck are used.\n" + "\n"+
                "SIGLEPLAYER MODES\n"+ "\n"+
                "Easy: When you press a card of your deck and didn't select any cards on the table, it auto-selects a 15 sum randommly in available. No timer\n" +"\n"+
                "Medium: When you press a card of your deck and didn't select any cards on the table, your card just goes to the table. No timer\n" +"\n"+
                "Hard: When you press a card of your deck and didn't select any cards on the table, your card just goes to the table. 15 timer\n" +"\n"+
                "PEDRADO: When you press a card of your deck and didn't select any cards on the table, your card just goes to the table. 10 timer\n" +"\n"+
                "\nEnjoy the game!";

        Label rulesLabel = new Label(rulesText, skin);
        rulesLabel.setWrap(true);

        ScrollPane scrollPane = new ScrollPane(rulesLabel, skin);
        scrollPane.setFadeScrollBars(false);
        root.add(scrollPane).width(Gdx.graphics.getWidth() * 0.8f)
            .height(Gdx.graphics.getHeight() * 0.6f)
            .padBottom(20).row();

        // Back button
        TextButton backBtn = new TextButton("Back", skin);
        backBtn.addListener(event -> {
            if (backBtn.isPressed()) {
                game.setScreen(new HomeScreenUI(game));
                return true;
            }
            return false;
        });

        root.add(backBtn).padTop(10);
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
