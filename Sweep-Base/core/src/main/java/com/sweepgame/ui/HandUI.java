package com.sweepgame.ui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.sweepgame.game.Card;
import com.sweepgame.game.Player;
import com.sweepgame.game.SweepGameUI;
import com.sweepgame.game.SweepLogic;
import com.sweepgame.utils.LayoutHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HandUI {
    private static final Logger logger = LoggerFactory.getLogger(HandUI.class);

    private Table table;
    private Player player;
    private SweepLogic gameLogic;
    private TableUI tableUI;
    private Runnable onCardPlayed;
    private SweepGameUI gameUI;
    private com.sweepgame.game.DifficultyConfig difficultyConfig;

    public HandUI(SweepGameUI gameUI, Player player, SweepLogic gameLogic, TableUI tableUI, Runnable onCardPlayed, com.sweepgame.game.DifficultyConfig difficultyConfig) {
        this.gameUI = gameUI;
        this.player = player;
        this.gameLogic = gameLogic;
        this.tableUI = tableUI;
        this.onCardPlayed = onCardPlayed;
        this.difficultyConfig = difficultyConfig;

        table = new Table();
        table.bottom();
    }

    public Table getTable() {
        return table;
    }

    private Image createCardImage(Card c) {
        try {
            Texture texture = new Texture("cards/" + c.getImageName());
            Image img = new Image(texture);

        img.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Block input during AI turns
                if (gameUI.isAITurnInProgress()) {
                    logger.debug("Card click blocked: AI turn in progress");
                    return;
                }
                
                logger.debug("Player clicked card: {}", c);
                List<Card> selected = tableUI.getSelectedCards();
                
                // Easy mode: Auto-select if no manual selection
                if (selected.isEmpty() && difficultyConfig.hasAutoSelection()) {
                    selected = gameLogic.findRandomValidSum15(c);
                    if (!selected.isEmpty()) {
                        logger.debug("Auto-selected {} cards for sum 15", selected.size());
                    }
                }
                
                if (!selected.isEmpty()) {
                    // Use manual or auto-selected capture
                    logger.debug("Playing card {} with {} selected table cards", c, selected.size());
                    gameLogic.playCardWithSelection(player, c, selected);
                    tableUI.clearSelection();
                } else {
                    // No valid selection - card goes to table
                    logger.debug("Playing card {} to table (no capture)", c);
                    gameLogic.playCardWithSelection(player, c, selected);
                }
                
                // Trigger animation for collected cards
                List<Card> collected = gameLogic.getLastCollectedCards();
                List<Card> cardsToAnimate;
                boolean isCapture = !collected.isEmpty();

                if (isCapture) {
                    cardsToAnimate = new ArrayList<>(collected);
                } else {cardsToAnimate = List.of(c);}

                animateCollectedCards(cardsToAnimate, isCapture);

                if (onCardPlayed != null) onCardPlayed.run();
            }
        });

        return img;
        } catch (Exception e) {
            logger.error("Error creating card image for: {}", c.getImageName(), e);
            // Return a placeholder or rethrow
            throw new RuntimeException("Failed to load card texture: " + c.getImageName(), e);
        }
    }

    private void animateCollectedCards(List<Card> cards, boolean moveToPlayer) {
        Stage stage = table.getStage();
        if (stage == null) {
            logger.warn("Cannot animate cards: stage is null");
            return;
        }
        
        logger.debug("Animating {} cards, capture={}", cards.size(), moveToPlayer);

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
            try {
                Texture texture = new Texture("cards/" + card.getImageName());
                Image img = new Image(texture);
            LayoutHelper layout = LayoutHelper.getInstance();
            img.setSize(layout.getHandCardWidth(), layout.getHandCardHeight());
            img.setPosition(startX, startY);
            stage.addActor(img);

            float targetX = centerX - ((n - 1) * spacing) / 2f + i * spacing;
            float targetY = centerY;

            if (moveToPlayer) {
                // Capture: move to center, pause, then fly to player
                img.addAction(Actions.sequence(
                    Actions.moveTo(targetX, targetY, 1f),
                    Actions.delay(1f),
                    Actions.moveTo(playerX, playerY, 1f),
                    Actions.run(() -> img.remove())
                ));
            } else {
                // Not a capture: just move to table center and stay
                img.addAction(Actions.sequence(
                    Actions.moveTo(targetX, targetY, 1f),
                    Actions.run(() -> img.remove()) // optional: remove after animation
                ));
            }
            } catch (Exception e) {
                logger.error("Error loading texture for animation: {}", card.getImageName(), e);
            }
        }
    }

    public void update() {
        logger.debug("Updating hand UI for player {}, {} cards", player.getName(), player.getHand().size());
        table.clear();
        LayoutHelper layout = LayoutHelper.getInstance();
        float handWidth = layout.getHandCardWidth();
        float handHeight = layout.getHandCardHeight();
        float padding = layout.getStandardPadding();
        
        for (Card c : player.getHand()) {
            table.add(createCardImage(c)).size(handWidth, handHeight).pad(padding);
        }
    }
}
