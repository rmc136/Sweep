package com.sweepgame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.sweepgame.ui.HomeScreenUI;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
// ...
public class SweepGame extends Game {
    private SpriteBatch batch;
    // private Texture image; // <- REMOVER OU COMENTAR

    @Override
    public void create() {
        // Inicializa o batch apenas se for usado noutras Screens (bom manter por agora)
        batch = new SpriteBatch();
        setScreen(new HomeScreenUI(this));
        // image = new Texture("icon.png"); // <- REMOVER OU COMENTAR
    }

    @Override
    public void render() {
        // Chamar super.render() é suficiente quando se usa setScreen().
        // O HomeScreenUI.render() será chamado automaticamente.
        super.render();

        // CÓDIGO DE DESENHO REDUNDANTE REMOVIDO:
        /*
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        batch.begin();
        batch.draw(image, 140, 210);
        batch.end();
        */
    }

    @Override
    public void dispose() {
        // Se removeu a Texture image, remova a sua disposição.
        batch.dispose();
        // image.dispose(); // <- REMOVER OU COMENTAR
        super.dispose();
    }
}
