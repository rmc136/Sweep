package com.sweepgame.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import com.sweepgame.cards.Player;

public class PlayerSeatUI {

    private Table rootTable;
    private Table leftTable;
    private Table rightTable;
    private Label leftPlayerLabel;
    private Label rightPlayerLabel;
    private Player leftPlayer;
    private Player rightPlayer;

    private Texture backTexture;

    private final float cardWidth = 80;
    private final float cardHeight = 100;

    public PlayerSeatUI(Skin skin, Player left, Player right) {
        rootTable = new Table();
        rootTable.setFillParent(true);

        leftPlayer = left;
        rightPlayer = right;

        leftPlayerLabel = new Label(left.getName(), skin);
        rightPlayerLabel = new Label(right.getName(), skin);

        backTexture = new Texture("cards/back_card.png");

        // Left side (name above, cards below)
        leftTable = new Table();
        leftTable.center().left();
        leftTable.add(leftPlayerLabel).padBottom(10).row();

        // Right side (name above, cards below)
        rightTable = new Table();
        rightTable.center().right();
        rightTable.add(rightPlayerLabel).padBottom(10).row();

        // Place left and right tables at screen borders
        rootTable.add(leftTable).expand().center().left();
        rootTable.add().expand(); // spacer
        rootTable.add(rightTable).expand().center().right();
    }

    public Table getTable() {
        return rootTable;
    }

    public void update() {
        // Left side update
        leftTable.clearChildren();
        leftTable.add(leftPlayerLabel).padBottom(10).row();
        for (int i = 0; i < leftPlayer.getHand().size(); i++) {
            Image cardBack = new Image(backTexture);
            cardBack.setScaling(Scaling.fit);
            cardBack.setSize(cardWidth, cardHeight);
            leftTable.add(cardBack).size(cardWidth, cardHeight).pad(0);
        }

        // Right side update
        rightTable.clearChildren();
        rightTable.add(rightPlayerLabel).padBottom(10).row();
        for (int i = 0; i < rightPlayer.getHand().size(); i++) {
            Image cardBack = new Image(backTexture);
            cardBack.setScaling(Scaling.fit);
            cardBack.setSize(cardWidth, cardHeight);
            rightTable.add(cardBack).size(cardWidth, cardHeight).pad(0);
        }
    }
}
