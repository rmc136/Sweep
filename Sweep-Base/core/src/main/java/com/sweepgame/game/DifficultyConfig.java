package com.sweepgame.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DifficultyConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DifficultyConfig.class);
    
    private final String difficulty;
    
    public DifficultyConfig(String difficulty) {
        this.difficulty = difficulty;
        logger.info("DifficultyConfig created: difficulty={}, autoSelection={}, timer={}", 
                   difficulty, hasAutoSelection(), hasTimer());
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
