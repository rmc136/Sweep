package com.sweepgame.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
// NOTA: NENHUM import de FreeType aqui!

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
        // VERSÃO EXCLUSIVA HTML (GWT)
        // Aqui não precisamos de verificar Gdx.app.getType() porque este ficheiro
        // só será lido pelo módulo HTML.
        
        try {
            // Tenta carregar as fontes Bitmap (.fnt)
            // Certifica-te que tens lsans-18.fnt, etc, na pasta assets
            defaultFont = new BitmapFont(Gdx.files.internal("lsans-18.fnt"));
            largeFont = new BitmapFont(Gdx.files.internal("lsans-24.fnt"));
            smallFont = new BitmapFont(Gdx.files.internal("lsans-14.fnt"));
            
            defaultFont.setColor(Color.WHITE);
            largeFont.setColor(Color.WHITE);
            smallFont.setColor(Color.WHITE);
        } catch (Exception e) {
            // Fallback se os ficheiros .fnt não existirem, para o jogo não crashar
            System.out.println("ERRO: Fontes .fnt não encontradas. Usando fonte padrão do sistema.");
            defaultFont = new BitmapFont();
            largeFont = new BitmapFont();
            smallFont = new BitmapFont();
            defaultFont.setColor(Color.WHITE);
            largeFont.setColor(Color.WHITE);
            smallFont.setColor(Color.WHITE);
        }
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
    
    public BitmapFont createTimerFont() {
        try {
            BitmapFont font = new BitmapFont(Gdx.files.internal("lsans-24.fnt"));
            font.setColor(Color.WHITE);
            return font;
        } catch (Exception e) {
            System.out.println("ERRO: Fontes .fnt não encontradas. Usando fonte padrão do sistema.");
            BitmapFont font = new BitmapFont();
            font.getData().setScale(2.0f);
            font.setColor(Color.WHITE);
            return font;
        }
    }
    
    public void dispose() {
        if (defaultFont != null) defaultFont.dispose();
        if (largeFont != null) largeFont.dispose();
        if (smallFont != null) smallFont.dispose();
    }
}