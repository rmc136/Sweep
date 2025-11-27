package com.sweepgame.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.sweepgame.cards.Player;
import com.sweepgame.utils.LayoutHelper;

import java.util.List;

public class ScoreUI {

    private Table table;
    private Label[] playerScoreLabels;

    public ScoreUI(Skin skin, List<Player> players) {
        table = new Table();
        table.top();

        int playerCount = players.size();
        playerScoreLabels = new Label[playerCount];

        LayoutHelper layout = LayoutHelper.getInstance();

        for (int i = 0; i < playerCount; i++) {
            Player p = players.get(i);
            playerScoreLabels[i] = new Label(formatScore(p), skin);
            
            if (layout.isMobile()) {
                playerScoreLabels[i].setWrap(true);
                playerScoreLabels[i].setFontScale(0.8f);
                table.add(playerScoreLabels[i]).width(200).pad(5);
            } else {
                table.add(playerScoreLabels[i]).pad(10);
            }
        }
    }

    public Table getTable() {
        return table;
    }

    public void update(List<Player> players) {
        for (int i = 0; i < playerScoreLabels.length; i++) {
            playerScoreLabels[i].setText(formatScore(players.get(i)));
        }
    }

    private String formatScore(Player p) {
        LayoutHelper layout = LayoutHelper.getInstance();
        if (layout.isMobile()) {
            return p.getName() + "\nPts: " + p.calculatePoints() + "\nSweeps: " + p.getBrushes() + "\nCards: " + p.getPointsStack().size();
        } else {
            return p.getName() + ": " + p.calculatePoints() + " pts, Sweeps: " + p.getBrushes() + ", Cards: " + p.getPointsStack().size();
        }
    }
}
