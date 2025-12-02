package com.sweepgame.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

public class FontManager {
    private static FontManager instance;
    private BitmapFont defaultFont;
    private BitmapFont largeFont;
    private BitmapFont smallFont;
    
    private FontManager() {
        generateFonts();
    }
    
    public static FontManager getInstance() {
        if (instance == null) {
            instance = new FontManager();
        }
        return instance;
    }
    
    private void generateFonts() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("lsans.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        
        // Default font - 18px
        parameter.size = 18;
        parameter.color = Color.WHITE;
        parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        defaultFont = generator.generateFont(parameter);
        
        // Large font - 24px (for scores)
        parameter.size = 24;
        largeFont = generator.generateFont(parameter);
        
        // Small font - 14px
        parameter.size = 14;
        smallFont = generator.generateFont(parameter);
        
        generator.dispose();
    }
    
    public BitmapFont getDefaultFont() {
        return defaultFont;
    }
    
    public BitmapFont getLargeFont() {
        return largeFont;
    }
    
    public BitmapFont getSmallFont() {
        return smallFont;
    }
    
    public void dispose() {
        if (defaultFont != null) defaultFont.dispose();
        if (largeFont != null) largeFont.dispose();
        if (smallFont != null) smallFont.dispose();
    }
}
