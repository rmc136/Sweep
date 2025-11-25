package com.sweepgame.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.sweepgame.cards.Card;

import java.util.ArrayList;
import java.util.List;

public class TableUI {

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
        selectedCards.clear();
    }

    private Image createTableCardImage(Card c) {
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
                } else {
                    selectedCards.remove(c);
                    img.setColor(1f, 1f, 1f, 1f);
                }
            }
        });

        return img;
    }

    public void update(List<Card> tableCards) {
        table.clear();
        selectedCards.clear();

        Table cardsRow = new Table();
        cardsRow.center();

        float cardWidth = 125f;
        float cardHeight = 150f;

        for (Card c : tableCards) {
            Image img = createTableCardImage(c);
            cardsRow.add(img).size(cardWidth, cardHeight).pad(2);
        }

        table.add(cardsRow).center();
    }
}
