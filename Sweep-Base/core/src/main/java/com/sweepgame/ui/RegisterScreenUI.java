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

public class RegisterScreenUI extends ScreenAdapter {
    private static final Logger logger = LoggerFactory.getLogger(RegisterScreenUI.class);

    private final SweepGame game;
    private Stage stage;
    private Skin skin;
    private TextField usernameField;
    private TextField emailField;
    private TextField passwordField;
    private TextField confirmPasswordField;
    private Label errorLabel;

    private final AuthService authService;
    private final LayoutHelper layoutHelper;

    public RegisterScreenUI(SweepGame game) {
        this.game = game;
        this.authService = game.getAuthService();
        this.layoutHelper = LayoutHelper.getInstance();
        logger.info("RegisterScreenUI initialized");
    }

    @Override
    public void show() {
        try {
            float width = layoutHelper.getViewportWidth();
            float height = layoutHelper.getViewportHeight();

            stage = new Stage(new FitViewport(width, height));
            skin = new Skin(Gdx.files.internal("uiskin.json"));

            // Replace bitmap fonts
            FontManager fontManager = FontManager.getInstance();
            skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class);
            skin.add("default-font", fontManager.getLargeFont(), com.badlogic.gdx.graphics.g2d.BitmapFont.class);

            Gdx.input.setInputProcessor(stage);

            Table root = new Table();
            root.setFillParent(true);
            stage.addActor(root);

            // Title
            Label titleLabel = new Label("REGISTER", skin);
            root.add(titleLabel).padBottom(layoutHelper.scale(40)).colspan(2).row();

            // Username field
            Label usernameLabel = new Label("Username:", skin);
            usernameField = new TextField("", skin);
            usernameField.setMessageText("Choose username");

            root.add(usernameLabel).padRight(layoutHelper.scale(15)).padBottom(layoutHelper.scale(15));
            root.add(usernameField).width(layoutHelper.scale(300)).height(layoutHelper.scale(40))
                    .padBottom(layoutHelper.scale(15)).row();

            // Email field
            Label emailLabel = new Label("Email:", skin);
            emailField = new TextField("", skin);
            emailField.setMessageText("Enter email");

            root.add(emailLabel).padRight(layoutHelper.scale(15)).padBottom(layoutHelper.scale(15));
            root.add(emailField).width(layoutHelper.scale(300)).height(layoutHelper.scale(40))
                    .padBottom(layoutHelper.scale(15)).row();

            // Password field
            Label passwordLabel = new Label("Password:", skin);
            passwordField = new TextField("", skin);
            passwordField.setMessageText("Choose password");
            passwordField.setPasswordMode(true);
            passwordField.setPasswordCharacter('*');

            root.add(passwordLabel).padRight(layoutHelper.scale(15)).padBottom(layoutHelper.scale(15));
            root.add(passwordField).width(layoutHelper.scale(300)).height(layoutHelper.scale(40))
                    .padBottom(layoutHelper.scale(15)).row();

            // Confirm password field
            Label confirmLabel = new Label("Confirm:", skin);
            confirmPasswordField = new TextField("", skin);
            confirmPasswordField.setMessageText("Confirm password");
            confirmPasswordField.setPasswordMode(true);
            confirmPasswordField.setPasswordCharacter('*');

            root.add(confirmLabel).padRight(layoutHelper.scale(15)).padBottom(layoutHelper.scale(30));
            root.add(confirmPasswordField).width(layoutHelper.scale(300)).height(layoutHelper.scale(40))
                    .padBottom(layoutHelper.scale(30)).row();

            // Error label
            errorLabel = new Label("", skin);
            errorLabel.setColor(Color.RED);
            errorLabel.setVisible(false);
            root.add(errorLabel).colspan(2).padBottom(layoutHelper.scale(15)).row();

            // Register button
            TextButton registerButton = new TextButton("REGISTER", skin);
            registerButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    attemptRegister();
                }
            });
            root.add(registerButton).width(layoutHelper.scale(200)).height(layoutHelper.scale(50)).colspan(2)
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

            logger.info("RegisterScreenUI screen shown");
        } catch (Exception e) {
            logger.error("Error showing register screen", e);
        }
    }

    private void attemptRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        logger.info("Register attempt for username: {}", username);

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters");
            return;
        }

        if (!email.contains("@")) {
            showError("Please enter a valid email");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        errorLabel.setVisible(false);

        authService.register(username, email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(AuthResponseDTO response) {
                logger.info("Registration successful for user: {}", response.getUsername());
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
        logger.warn("Registration error: {}", message);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.4f, 1);
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
        logger.info("RegisterScreenUI disposed");
    }
}
