package com.sweepgame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sweepgame.SweepGame;
import com.sweepgame.network.AuthService;
import com.sweepgame.network.dto.AuthResponseDTO;
import com.sweepgame.utils.FontManager;
import com.sweepgame.utils.LayoutHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginScreenUI extends ScreenAdapter {
    private static final Logger logger = LoggerFactory.getLogger(LoginScreenUI.class);

    private final SweepGame game;
    private Stage stage;
    private Skin skin;
    private TextField usernameField;
    private TextField passwordField;
    private Label errorLabel;

    private final AuthService authService;
    private final LayoutHelper layoutHelper;
    // Removed old VIRTUAL constants as we use LayoutHelper

    public LoginScreenUI(SweepGame game) {
        this.game = game;
        this.authService = game.getAuthService();
        this.layoutHelper = LayoutHelper.getInstance();
        logger.info("LoginScreenUI initialized");
    }

    @Override
    public void show() {
        try {
            float width = layoutHelper.getViewportWidth();
            float height = layoutHelper.getViewportHeight();

            stage = new Stage(new FitViewport(width, height));
            skin = new Skin(Gdx.files.internal("uiskin.json"));

            // Replace bitmap fonts with crisp TTF fonts
            FontManager fontManager = FontManager.getInstance();
            skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class);
            skin.add("default-font", fontManager.getLargeFont(), com.badlogic.gdx.graphics.g2d.BitmapFont.class);

            Gdx.input.setInputProcessor(stage);

            Table root = new Table();
            root.setFillParent(true);
            stage.addActor(root);

            // Title
            Label titleLabel = new Label("LOGIN", skin);
            root.add(titleLabel).padBottom(layoutHelper.scale(40)).colspan(2).row();

            // Username field
            Label usernameLabel = new Label("Username:", skin);
            usernameField = new TextField("", skin);
            usernameField.setMessageText("Enter username");

            root.add(usernameLabel).padRight(layoutHelper.scale(15)).padBottom(layoutHelper.scale(15));
            root.add(usernameField).width(layoutHelper.scale(300)).height(layoutHelper.scale(40))
                    .padBottom(layoutHelper.scale(15)).row();

            // Password field
            Label passwordLabel = new Label("Password:", skin);
            passwordField = new TextField("", skin);
            passwordField.setMessageText("Enter password");
            passwordField.setPasswordMode(true);
            passwordField.setPasswordCharacter('*');

            root.add(passwordLabel).padRight(layoutHelper.scale(15)).padBottom(layoutHelper.scale(30));
            root.add(passwordField).width(layoutHelper.scale(300)).height(layoutHelper.scale(40))
                    .padBottom(layoutHelper.scale(30)).row();

            // Error label
            errorLabel = new Label("", skin);
            errorLabel.setColor(Color.RED);
            errorLabel.setVisible(false);
            root.add(errorLabel).colspan(2).padBottom(layoutHelper.scale(15)).row();

            // Login button
            TextButton loginButton = new TextButton("LOGIN", skin);
            loginButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    attemptLogin();
                }
            });
            root.add(loginButton).width(layoutHelper.scale(200)).height(layoutHelper.scale(50)).colspan(2)
                    .padBottom(layoutHelper.scale(20)).row();

            // Back button
            TextButton backButton = new TextButton("BACK", skin);
            backButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    logger.info("Back to welcome screen");
                    game.setScreen(new WelcomeScreenUI(game));
                }
            });
            root.add(backButton).width(layoutHelper.scale(150)).height(layoutHelper.scale(40)).colspan(2);

            logger.info("LoginScreenUI screen shown");
        } catch (Exception e) {
            logger.error("Error showing login screen", e);
        }
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        logger.info("Login attempt for username: {}", username);

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        // Disable UI while loading (simple way)
        errorLabel.setVisible(false);

        authService.login(username, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(AuthResponseDTO response) {
                logger.info("Login successful for user: {}", response.getUsername());
                Gdx.app.postRunnable(() -> {
                    authService.startTokenRefreshScheduler();
                    game.setScreen(new HomeScreenUI(game));
                });
            }

            @Override
            public void onError(String message) {
                Gdx.app.postRunnable(() -> showError(message));
            }
        });
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        logger.warn("Login error: {}", message);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.4f, 1); // Match HomeScreenUI
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (stage != null) {
            stage.act(delta);
            stage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
        if (skin != null) {
            skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class);
            skin.dispose();
        }
        logger.info("LoginScreenUI disposed");
    }
}
