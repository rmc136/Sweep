package com.sweepgame.ui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.sweepgame.cards.Card;
import com.sweepgame.utils.LayoutHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TableUI {
    private static final Logger logger = LoggerFactory.getLogger(TableUI.class);

    private Table table;
    private List<Card> selectedCards = new ArrayList<>();

    public TableUI() {
        table = new Table();
        table.center();
    }

    public Table getTable() {
        return table;
    }

    public List<Card> getSelectedCards() {
        return new ArrayList<>(selectedCards); // defensive copy
    }

    public void clearSelection() {
        if (!selectedCards.isEmpty()) {
            logger.debug("Clearing {} selected cards", selectedCards.size());
        }
        selectedCards.clear();
    }

    private Image createTableCardImage(Card c) {
        try {
            Texture texture = new Texture("cards/" + c.getImageName());
            Image img = new Image(texture);

        img.addListener(new ClickListener() {
            private boolean selected = false;

            @Override
            public void clicked(InputEvent event, float x, float y) {
                selected = !selected;
                if (selected) {
                    selectedCards.add(c);
                    img.setColor(0.5f, 1f, 0.5f, 1f); // green tint
                    logger.debug("Card selected: {}, total selected: {}", c, selectedCards.size());
                } else {
                    selectedCards.remove(c);
                    img.setColor(1f, 1f, 1f, 1f);
                    logger.debug("Card deselected: {}, total selected: {}", c, selectedCards.size());
                }
            }
        });

        return img;
        } catch (Exception e) {
            logger.error("Error creating table card image for: {}", c.getImageName(), e);
            throw new RuntimeException("Failed to load card texture: " + c.getImageName(), e);
        }
    }

    public void update(List<Card> tableCards) {
        logger.debug("Updating table UI with {} cards", tableCards.size());
        table.clear();
        selectedCards.clear();

        if (tableCards.isEmpty()) {
            logger.debug("Table is empty");
            return;
        }

        LayoutHelper layout = LayoutHelper.getInstance();
        
        // Fixed container size
        float containerWidth;
        float containerHeight;
        
        if (layout.isMobile()) {
            containerWidth = layout.getWidth(0.95f);
            containerHeight = layout.getHeight(0.4f);
            // Mobile: Align top with padding to push it down, instead of centering
            table.top().padTop(layout.getHeight(0.15f)); 
        } else {
            containerWidth = layout.getWidth(0.5f);
            containerHeight = layout.getHeight(0.6f);
            // Desktop: Center alignment
            table.center();
        }

        float maxOriginalWidth = layout.getTableCardWidth();
        
        int totalCards = tableCards.size();
        
        // OPTIMIZATION ALGORITHM:
        // Find the configuration (1 to 4 rows) that yields the LARGEST card size.
        
        int bestNumRows = 1;
        float bestCardWidth = 0;
        float bestCardHeight = 0;
        
        for (int r = 1; r <= 4; r++) {
            // Calculate columns needed for r rows
            int c = (int) Math.ceil((double) totalCards / r);
            
            // Calculate max card size for this configuration
            float availableW = containerWidth / c;
            float availableH = containerHeight / r;
            
            float w = availableW * 0.95f; // 5% padding
            float h = w * 1.4f;
            
            // Check height constraint
            if (h > availableH * 0.95f) {
                h = availableH * 0.95f;
                w = h / 1.4f;
            }
            
            // Cap at original size
            if (w > maxOriginalWidth) {
                w = maxOriginalWidth;
                h = w * 1.4f;
            }
            
            // Is this better?
            if (w > bestCardWidth) {
                bestCardWidth = w;
                bestCardHeight = h;
                bestNumRows = r;
            }
        }
                
        // Add cards using the best configuration
        int cardIndex = 0;
        for (int row = 0; row < bestNumRows; row++) {
            int remainingCards = totalCards - cardIndex;
            int remainingRows = bestNumRows - row;
            int cardsInThisRow = (int) Math.ceil((double) remainingCards / remainingRows);
            
            for (int col = 0; col < cardsInThisRow; col++) {
                if (cardIndex >= totalCards) break;
                
                Card c = tableCards.get(cardIndex);
                Image img = createTableCardImage(c);
                table.add(img).size(bestCardWidth, bestCardHeight).pad(2);
                cardIndex++;
            }
            table.row();
        }
    }
}
