package com.sweepgame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.sweepgame.ui.WelcomeScreenUI;
import com.sweepgame.network.AuthService;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class SweepGame extends Game {
    private SpriteBatch batch;
    private final AuthService authService;

    public SweepGame() {
        this.authService = new AuthService();
    }

    @Override
    public void create() {
        // Inicializa o batch apenas se for usado noutras Screens (bom manter por agora)
        batch = new SpriteBatch();
        
        // Start refresh scheduler if already logged in (e.g. app restart)
        if (authService.isLoggedIn()) {
            authService.startTokenRefreshScheduler();
        }
        
        setScreen(new WelcomeScreenUI(this));
    }
    
    public AuthService getAuthService() {
        return authService;
    }

    @Override
    public void render() {
        // Chamar super.render() é suficiente quando se usa setScreen().
        // O WelcomeScreenUI.render() será chamado automaticamente.
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        authService.stopTokenRefreshScheduler();
        super.dispose();
    }
}
