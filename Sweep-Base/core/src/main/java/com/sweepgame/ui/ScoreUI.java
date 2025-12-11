package com.sweepgame.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.sweepgame.game.Player;
import com.sweepgame.utils.LayoutHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ScoreUI {
    private static final Logger logger = LoggerFactory.getLogger(ScoreUI.class);

    private Table table;
    private Label[] playerScoreLabels;
    private com.sweepgame.game.TournamentManager tournamentManager;

    public ScoreUI(Skin skin, List<Player> players, com.sweepgame.game.TournamentManager tournamentManager) {
        // We no longer create a main table here, as labels will be distributed
        this.tournamentManager = tournamentManager;
        int playerCount = players.size();
        playerScoreLabels = new Label[playerCount];

        LayoutHelper layout = LayoutHelper.getInstance();

        for (int i = 0; i < playerCount; i++) {
            Player p = players.get(i);
            playerScoreLabels[i] = new Label(formatScore(p), skin);
            // TTF fonts from FontManager are already crisp, no scaling needed
        }
    }

    public Label getLabel(int playerIndex) {
        if (playerIndex >= 0 && playerIndex < playerScoreLabels.length) {
            return playerScoreLabels[playerIndex];
        }
        logger.warn("Invalid player index requested: {}", playerIndex);
        return null;
    }

    public void update(List<Player> players) {
        logger.debug("Updating scores for {} players", players.size());
        for (int i = 0; i < playerScoreLabels.length && i < players.size(); i++) {
            playerScoreLabels[i].setText(formatScore(players.get(i)));
        }
    }

    private String formatScore(Player p) {
        LayoutHelper layout = LayoutHelper.getInstance();
        int gamesWon = tournamentManager.getWins(p.getName());
        
        // Only show games won if not in single game mode
        String gamesWonText = tournamentManager.isSingleGame() ? "" : "Games: " + gamesWon + "/" + tournamentManager.getWinsNeeded() + "\n";
        
        // Use multi-line format for both mobile and desktop for better readability
        return p.getName() + "\n" + gamesWonText + "Pts: " + p.calculatePoints() + "\nSweeps: " + p.getBrushes() + "\nCards: " + p.getPointsStack().size();
    }
}
