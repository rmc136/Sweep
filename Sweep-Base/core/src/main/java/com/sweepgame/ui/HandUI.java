package com.sweepgame.ui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.sweepgame.cards.Card;
import com.sweepgame.cards.Player;
import com.sweepgame.game.SweepLogic;

import java.util.ArrayList;
import java.util.List;

public class HandUI {

    private Table table;
    private Player player;
    private SweepLogic gameLogic;
    private TableUI tableUI; // <--- NEW
    private Runnable onCardPlayed;

    public HandUI(Player player, SweepLogic gameLogic, TableUI tableUI, Runnable onCardPlayed) {
        table = new Table();
        table.bottom();
        this.player = player;
        this.gameLogic = gameLogic;
        this.tableUI = tableUI; // <--- NEW
        this.onCardPlayed = onCardPlayed;
    }

    public Table getTable() {
        return table;
    }

    private Image createCardImage(Card c) {
        Texture texture = new Texture("cards/" + c.getImageName());
        Image img = new Image(texture);

        img.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                List<Card> selected = tableUI.getSelectedCards();
                if (!selected.isEmpty()) {
                    // Use manual capture
                    gameLogic.playCardWithSelection(player, c, selected);
                    tableUI.clearSelection();
                    // reset highlight
                } else {
                    // Default auto logic
                    gameLogic.playCardWithSelection(player, c, selected);
                }
                // Trigger animation for collected cards
                List<Card> collected = gameLogic.getLastCollectedCards();
                List<Card> cardsToAnimate;// get cards collected this turn
                boolean isCapture = !collected.isEmpty();

                if (isCapture) {
                    cardsToAnimate = new ArrayList<>(collected);
                } else {cardsToAnimate = List.of(c);}

                animateCollectedCards(cardsToAnimate, isCapture);

                if (onCardPlayed != null) onCardPlayed.run();
            }
        });

        return img;
    }

    private void animateCollectedCards(List<Card> cards, boolean moveToPlayer) {
        Stage stage = table.getStage();
        if (stage == null) return;

        // Starting position: hand card
        Actor firstCardActor = table.getChildren().first();
        Vector2 startPos = table.localToStageCoordinates(new Vector2(firstCardActor.getX(), firstCardActor.getY()));
        float startX = startPos.x;
        float startY = startPos.y;

        // Table center
        Table tableParent = tableUI.getTable();
        float centerX = tableParent.getX() + tableParent.getWidth() / 2f;
        float centerY = tableParent.getY() + tableParent.getHeight() / 2f;

        // Player hand position
        float playerX = table.getX() + table.getWidth() / 2f - 40;
        float playerY = table.getY() + table.getHeight() / 2f - 50;

        int n = cards.size();
        float spacing = 30f; // horizontal spacing between cards at table center

        for (int i = 0; i < n; i++) {
            Card card = cards.get(i);
            Texture texture = new Texture("cards/" + card.getImageName());
            Image img = new Image(texture);
            boolean isMobile = Gdx.app.getType() == Application.ApplicationType.Android || Gdx.app.getType() == Application.ApplicationType.iOS;
            img.setSize(isMobile ? 140 : 120, isMobile ? 170 : 140);
            img.setPosition(startX, startY);
            stage.addActor(img);

            float targetX = centerX - ((n - 1) * spacing) / 2f + i * spacing;
            float targetY = centerY;

            if (moveToPlayer) {
                // Capture: move to center, pause, then fly to player
                img.addAction(Actions.sequence(
                    Actions.moveTo(targetX, targetY, 0.5f),
                    Actions.delay(1f),
                    Actions.moveTo(playerX, playerY, 0.5f),
                    Actions.run(() -> img.remove())
                ));
            } else {
                // Not a capture: just move to table center and stay
                img.addAction(Actions.sequence(
                    Actions.moveTo(targetX, targetY, 0.5f),
                    Actions.run(() -> img.remove()) // optional: remove after animation
                ));
            }
        }
    }

    public void update() {
        table.clear();
        boolean isMobile = Gdx.app.getType() == Application.ApplicationType.Android || Gdx.app.getType() == Application.ApplicationType.iOS;
        float handWidth = isMobile ? 100 : 80;
        float handHeight = isMobile ? 130 : 100;
        float padding = isMobile ? 3 : 2;
        for (Card c : player.getHand()) {
            table.add(createCardImage(c)).size(handWidth, handHeight).pad(padding);
        }
    }
}
