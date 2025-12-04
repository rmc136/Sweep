package com.sweepgame.game;

public class DifficultyConfig {
    
    private final String difficulty;
    
    public DifficultyConfig(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public boolean hasAutoSelection() {
        return "Easy".equalsIgnoreCase(difficulty);
    }
    
    public boolean hasTimer() {
        return "Hard".equalsIgnoreCase(difficulty) || "Pedrado".equalsIgnoreCase(difficulty);
    }
    
    public float getTimerSeconds() {
        if ("Hard".equalsIgnoreCase(difficulty)) {
            return 15f;
        } else if ("Pedrado".equalsIgnoreCase(difficulty)) {
            return 10f;
        }
        return 0f;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
}
