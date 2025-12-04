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
        
        parameter.size = 18;
        parameter.color = Color.WHITE;
        parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        defaultFont = generator.generateFont(parameter);
        
        parameter.size = 24;
        largeFont = generator.generateFont(parameter);
        
        parameter.size = 14;
        smallFont = generator.generateFont(parameter);
        
        generator.dispose();
    }
    
    private void checkFonts() {
        boolean needsRegeneration = false;
        
        if (defaultFont != null) {
            try {
                if (defaultFont.getRegion().getTexture() == null) needsRegeneration = true;
            } catch (Exception e) {
                needsRegeneration = true;
            }
        } else {
            needsRegeneration = true;
        }
        
        if (needsRegeneration) {
            generateFonts();
        }
    }

    public BitmapFont getDefaultFont() {
        checkFonts();
        return defaultFont;
    }
    
    public BitmapFont getLargeFont() {
        checkFonts();
        return largeFont;
    }
    
    public BitmapFont getSmallFont() {
        checkFonts();
        return smallFont;
    }
    
    /**
     * Generates a new instance of a large font for the timer.
     * The caller is responsible for disposing this font!
     */
    public BitmapFont createTimerFont() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("lsans.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = 32; // Large crisp size
        parameter.color = Color.WHITE;
        parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }
    
    public void dispose() {
        if (defaultFont != null) defaultFont.dispose();
        if (largeFont != null) largeFont.dispose();
        if (smallFont != null) smallFont.dispose();
    }
}
