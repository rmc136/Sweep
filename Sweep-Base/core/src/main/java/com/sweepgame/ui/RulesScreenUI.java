package com.sweepgame.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesScreenUI extends ScreenAdapter {
    private static final Logger logger = LoggerFactory.getLogger(RulesScreenUI.class);

    private final Game game;
    private Stage stage;
    private Skin skin;

    public RulesScreenUI(Game game) {
        logger.info("Initializing rules screen");
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
        Label title = new Label("Sweep Game Rules", skin);
        // No font scaling needed - TTF fonts are crisp
        root.top().add(title).padTop(20).padBottom(20).row();

        // Scrollable rules
        String rulesText =
            "BASIC RULES:\n"+ "\n"+
                "1. Each player takes turns playing a card.\n" +"\n"+
                "2. Each card is face value for the sums, except the Aces (which are 1), the Queens (which are 8), the Jacks (which are 9) and the Kings (which are 10).\n" +"\n"+
                "3. Try to collect cards that sum up to 15.\n" +"\n"+
                "4. You can only play one of the cards in your hand per turn, and try to combine it with the table cards to make 15 selecting the ones you want and prefer\n" +"\n"+
                "5. If you capture all table cards + your played card, you get a Sweep bonus (+1 point in the turn).\n" +"\n"+
                "6. Each Seven is worth 1 point and each diamond is worth a point, the 7 of diamonds is worth 2 points.\n" +"\n"+
                "7. The player with most points at the end wins!\n" + "\n"+
                "8. If no cards can be collected, your card stays on the table.\n" +"\n"+
                "9. Continue until all cards in deck are used.\n" + "\n"+
                "SIGLEPLAYER MODES\n"+ "\n"+
                "Easy: When you press a card of your deck and didn't select any cards on the table, it auto-selects a 15 sum randommly in available. No timer\n" +"\n"+
                "Medium: When you press a card of your deck and didn't select any cards on the table, your card just goes to the table. No timer\n" +"\n"+
                "Hard: When you press a card of your deck and didn't select any cards on the table, your card just goes to the table. 15 timer\n" +"\n"+
                "PEDRADO: When you press a card of your deck and didn't select any cards on the table, your card just goes to the table. 10 timer\n" +"\n"+
                "\nEnjoy the game!";

        Label rulesLabel = new Label(rulesText, skin);
        rulesLabel.setWrap(true);
        // No font scaling needed - TTF fonts are crisp

        ScrollPane scrollPane = new ScrollPane(rulesLabel, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false); // Enable vertical scrolling
        scrollPane.setOverscroll(false, false);
        
        // Use fixed proportions of viewport
        root.add(scrollPane).width(width * 0.85f)
            .height(height * 0.6f)
            .padBottom(10).expand().row();

        // Back button - ensure it's at the bottom
        TextButton backBtn = new TextButton("Back", skin);
        backBtn.addListener(event -> {
            if (backBtn.isPressed()) {
                game.setScreen(new HomeScreenUI(game));
                return true;
            }
            return false;
        });

        root.add(backBtn).padTop(10).padBottom(20);
        
        logger.debug("Rules screen initialized successfully");
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
        logger.debug("Disposing rules screen resources");
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }
}
