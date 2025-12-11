package com.sweepgame.ui;

import com.badlogic.gdx.graphics.Texture;
import com.sweepgame.utils.LayoutHelper;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import com.sweepgame.game.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerSeatUI {
    private static final Logger logger = LoggerFactory.getLogger(PlayerSeatUI.class);

    private Table rootTable;
    private Table leftTable;
    private Table rightTable;

    private Player leftPlayer;
    private Player rightPlayer;
    private Label leftScoreLabel;
    private Label rightScoreLabel;
    private Texture backTexture;


    
    // Removed hardcoded sizes
    // private final float cardWidth = 80;
    // private final float cardHeight = 100;

    public PlayerSeatUI(Skin skin, Player left, Player right, Label leftScore, Label rightScore) {
        rootTable = new Table();
        rootTable.setFillParent(true);

        leftPlayer = left;
        rightPlayer = right;

        backTexture = new Texture("cards/back_card.png");

        // Left side (name above, cards below)
        leftTable = new Table();
        // Fixed width, align top center
        leftTable.top(); 
        
        // Right side (name above, cards below)
        rightTable = new Table();
        // Fixed width, align top center
        rightTable.top();

        // Place left and right tables at screen borders
        // Place left and right tables at screen borders
        LayoutHelper layout = LayoutHelper.getInstance();
        
        if (layout.isMobile()) {
            // Mobile: Align to TOP with padding
            // Use fixed width for the seats to prevent shifting
            float seatWidth = layout.getSeatWidth();
            
            rootTable.add(leftTable).width(seatWidth).top().left().padTop(150);
            rootTable.add().expand(); // spacer
            rootTable.add(rightTable).width(seatWidth).top().right().padTop(150);
        } else {
            // Desktop: Center alignment
            float seatWidth = layout.getSeatWidth();

            rootTable.add(leftTable).width(seatWidth).expand().center().left();
            rootTable.add().expand(); // spacer
            rootTable.add(rightTable).width(seatWidth).expand().center().right();
        }
        
        logger.debug("PlayerSeatUI initialized for {} and {}", left.getName(), right.getName());
    }

    public Table getTable() {
        return rootTable;
    }

    public void setScoreLabels(Label left, Label right) {
        this.leftScoreLabel = left;
        this.rightScoreLabel = right;
    }

    public void update() {
        LayoutHelper layout = LayoutHelper.getInstance();
        float cardWidth = layout.getHandCardWidth(); 
        float cardHeight = layout.getHandCardHeight();

        // Left side update
        leftTable.clearChildren();
        // Always add Score and Name at the top
        leftTable.add(leftScoreLabel).padBottom(5).row();
        
        
        // Container for cards to keep them centered within the seat
        Table leftCardsTable = new Table();
        for (int i = 0; i < leftPlayer.getHand().size(); i++) {
            Image cardBack = new Image(backTexture);
            cardBack.setScaling(Scaling.fit);
            cardBack.setSize(cardWidth, cardHeight);
            leftCardsTable.add(cardBack).size(cardWidth, cardHeight).pad(2); // pad 2 for overlap/spacing
        }
        leftTable.add(leftCardsTable).row();

        // Right side update
        rightTable.clearChildren();
        // Always add Score and Name at the top
        rightTable.add(rightScoreLabel).padBottom(5).row();
        
        // Container for cards
        Table rightCardsTable = new Table();
        for (int i = 0; i < rightPlayer.getHand().size(); i++) {
            Image cardBack = new Image(backTexture);
            cardBack.setScaling(Scaling.fit);
            cardBack.setSize(cardWidth, cardHeight);
            rightCardsTable.add(cardBack).size(cardWidth, cardHeight).pad(2);
        }
        rightTable.add(rightCardsTable).row();
        
        logger.debug("Updated player seats: left={} cards, right={} cards", 
                    leftPlayer.getHand().size(), rightPlayer.getHand().size());
    }
}

