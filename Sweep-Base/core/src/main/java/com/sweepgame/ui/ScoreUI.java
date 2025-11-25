package com.sweepgame.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.sweepgame.cards.Player;

import java.util.List;

public class ScoreUI {

    private Table table;
    private Label[] playerScoreLabels;

    public ScoreUI(Skin skin, List<Player> players) {
        table = new Table();
        table.top();

        int playerCount = players.size();
        playerScoreLabels = new Label[playerCount];

        for (int i = 0; i < playerCount; i++) {
            Player p = players.get(i);
            playerScoreLabels[i] = new Label(formatScore(p), skin);
            table.add(playerScoreLabels[i]).pad(10);
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
        return p.getName() + ": " + p.calculatePoints() + " pts, Brushes: " + p.getBrushes() + ", Cards: " + p.getPointsStack().size();
    }
}
