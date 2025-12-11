package com.sweepgame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.sweepgame.SweepGame;
import com.sweepgame.network.AuthService;
import com.sweepgame.utils.FontManager;
import com.sweepgame.utils.LayoutHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterScreenUI extends ScreenAdapter {
    private static final Logger logger = LoggerFactory.getLogger(RegisterScreenUI.class);
    
    private final SweepGame game;
    private Stage stage;
    private TextField usernameField;
    private TextField emailField;
    private TextField passwordField;
    private TextField confirmPasswordField;
    private Label errorLabel;
    
    private final AuthService authService;
    
    public RegisterScreenUI(SweepGame game) {
        this.game = game;
        this.authService = game.getAuthService();
        logger.info("RegisterScreenUI initialized");
    }
    
    @Override
    public void show() {
        // ... (UI setup code reuse existing but update if needed, but here we focus on integration)
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);
        
        // Title
        BitmapFont titleFont = FontManager.getInstance().getLargeFont();
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
        Label titleLabel = new Label("REGISTER", titleStyle);
        root.add(titleLabel).padBottom(LayoutHelper.getInstance().scale(60)).colspan(2).row();
        
        // Field styles
        BitmapFont labelFont = FontManager.getInstance().getDefaultFont();
        Label.LabelStyle labelStyle = new Label.LabelStyle(labelFont, Color.WHITE);
        TextField.TextFieldStyle fieldStyle = createFieldStyle();
        
        // Username field
        Label usernameLabel = new Label("Username:", labelStyle);
        root.add(usernameLabel).padRight(LayoutHelper.getInstance().scale(20)).padBottom(LayoutHelper.getInstance().scale(20));
        
        usernameField = new TextField("", fieldStyle);
        usernameField.setMessageText("Choose username");
        root.add(usernameField).width(LayoutHelper.getInstance().scale(400)).height(LayoutHelper.getInstance().scale(60))
            .padBottom(LayoutHelper.getInstance().scale(20)).row();
        
        // Email field
        Label emailLabel = new Label("Email:", labelStyle);
        root.add(emailLabel).padRight(LayoutHelper.getInstance().scale(20)).padBottom(LayoutHelper.getInstance().scale(20));
        
        emailField = new TextField("", fieldStyle);
        emailField.setMessageText("Enter email");
        root.add(emailField).width(LayoutHelper.getInstance().scale(400)).height(LayoutHelper.getInstance().scale(60))
            .padBottom(LayoutHelper.getInstance().scale(20)).row();
        
        // Password field
        Label passwordLabel = new Label("Password:", labelStyle);
        root.add(passwordLabel).padRight(LayoutHelper.getInstance().scale(20)).padBottom(LayoutHelper.getInstance().scale(20));
        
        passwordField = new TextField("", fieldStyle);
        passwordField.setMessageText("Choose password");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        root.add(passwordField).width(LayoutHelper.getInstance().scale(400)).height(LayoutHelper.getInstance().scale(60))
            .padBottom(LayoutHelper.getInstance().scale(20)).row();
        
        // Confirm password field
        Label confirmLabel = new Label("Confirm:", labelStyle);
        root.add(confirmLabel).padRight(LayoutHelper.getInstance().scale(20)).padBottom(LayoutHelper.getInstance().scale(40));
        
        confirmPasswordField = new TextField("", fieldStyle);
        confirmPasswordField.setMessageText("Confirm password");
        confirmPasswordField.setPasswordMode(true);
        confirmPasswordField.setPasswordCharacter('*');
        root.add(confirmPasswordField).width(LayoutHelper.getInstance().scale(400)).height(LayoutHelper.getInstance().scale(60))
            .padBottom(LayoutHelper.getInstance().scale(40)).row();
        
        // Error label
        BitmapFont errorFont = FontManager.getInstance().getSmallFont();
        Label.LabelStyle errorStyle = new Label.LabelStyle(errorFont, Color.RED);
        errorLabel = new Label("", errorStyle);
        errorLabel.setVisible(false);
        root.add(errorLabel).colspan(2).padBottom(LayoutHelper.getInstance().scale(20)).row();
        
        // Register button
        TextButton registerButton = createButton("REGISTER");
        registerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                attemptRegister();
            }
        });
        root.add(registerButton).width(LayoutHelper.getInstance().scale(300)).height(LayoutHelper.getInstance().scale(80))
            .colspan(2).padBottom(LayoutHelper.getInstance().scale(30)).row();
        
        // Back button
        TextButton backButton = createButton("BACK");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                logger.info("Back to welcome screen");
                game.setScreen(new WelcomeScreenUI(game));
            }
        });
        root.add(backButton).width(LayoutHelper.getInstance().scale(200)).height(LayoutHelper.getInstance().scale(60)).colspan(2);
        
        logger.info("RegisterScreenUI screen shown");
    }
    
    private TextField.TextFieldStyle createFieldStyle() {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = FontManager.getInstance().getDefaultFont();
        style.fontColor = Color.WHITE;
        style.messageFontColor = Color.LIGHT_GRAY;
        style.cursor = createDrawable(Color.WHITE);
        style.selection = createDrawable(new Color(0.3f, 0.3f, 0.5f, 0.5f));
        style.background = createDrawable(new Color(0.2f, 0.2f, 0.25f, 0.5f));
        return style;
    }
    
    // ... helper methods (createDrawable, createButton) existing ...
    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createDrawable(Color color) {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(texture));
    }
    
    private TextButton createButton(String text) {
        BitmapFont font = FontManager.getInstance().getDefaultFont();
        
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.downFontColor = Color.GRAY;
        
        return new TextButton(text, style);
    }
    
    private void attemptRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        logger.info("Register attempt for username: {}", username);
        
        // Validation
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
        
        // Disable UI
        errorLabel.setVisible(false);
        
        authService.register(username, email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(com.sweepgame.network.dto.AuthResponseDTO response) {
                logger.info("Registration successful for user: {}", response.getUsername());
                // Start refresh scheduler on successful registration (as it auto-logins)
                authService.startTokenRefreshScheduler();
                // Let's go to Home since server usually returns token on register too
                game.setScreen(new HomeScreenUI(game));
            }
            
            @Override
            public void onError(String message) {
                showError(message);
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
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        stage.act(delta);
        stage.draw();
    }
    
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    
    @Override
    public void dispose() {
        stage.dispose();
        logger.info("RegisterScreenUI disposed");
    }
}
