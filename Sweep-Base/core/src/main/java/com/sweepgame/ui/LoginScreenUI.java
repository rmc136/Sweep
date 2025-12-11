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

public class LoginScreenUI extends ScreenAdapter {
    private static final Logger logger = LoggerFactory.getLogger(LoginScreenUI.class);
    
    private final SweepGame game;
    private Stage stage;
    private TextField usernameField;
    private TextField passwordField;
    private Label errorLabel;
    
    private final AuthService authService;
    
    public LoginScreenUI(SweepGame game) {
        this.game = game;
        this.authService = game.getAuthService();
        logger.info("LoginScreenUI initialized");
    }
    
    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);
        
        // Title
        BitmapFont titleFont = FontManager.getInstance().getLargeFont();
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
        Label titleLabel = new Label("LOGIN", titleStyle);
        root.add(titleLabel).padBottom(LayoutHelper.getInstance().scale(60)).colspan(2).row();
        
        // Username field
        BitmapFont labelFont = FontManager.getInstance().getDefaultFont();
        Label.LabelStyle labelStyle = new Label.LabelStyle(labelFont, Color.WHITE);
        
        Label usernameLabel = new Label("Username:", labelStyle);
        root.add(usernameLabel).padRight(LayoutHelper.getInstance().scale(20)).padBottom(LayoutHelper.getInstance().scale(20));
        
        TextField.TextFieldStyle fieldStyle = createFieldStyle();
        usernameField = new TextField("", fieldStyle);
        usernameField.setMessageText("Enter username");
        root.add(usernameField).width(LayoutHelper.getInstance().scale(400)).height(LayoutHelper.getInstance().scale(60))
            .padBottom(LayoutHelper.getInstance().scale(20)).row();
        
        // Password field
        Label passwordLabel = new Label("Password:", labelStyle);
        root.add(passwordLabel).padRight(LayoutHelper.getInstance().scale(20)).padBottom(LayoutHelper.getInstance().scale(40));
        
        passwordField = new TextField("", fieldStyle);
        passwordField.setMessageText("Enter password");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        root.add(passwordField).width(LayoutHelper.getInstance().scale(400)).height(LayoutHelper.getInstance().scale(60))
            .padBottom(LayoutHelper.getInstance().scale(40)).row();
        
        // Error label
        BitmapFont errorFont = FontManager.getInstance().getSmallFont();
        Label.LabelStyle errorStyle = new Label.LabelStyle(errorFont, Color.RED);
        errorLabel = new Label("", errorStyle);
        errorLabel.setVisible(false);
        root.add(errorLabel).colspan(2).padBottom(LayoutHelper.getInstance().scale(20)).row();
        
        // Login button
        TextButton loginButton = createButton("LOGIN");
        loginButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                attemptLogin();
            }
        });
        root.add(loginButton).width(LayoutHelper.getInstance().scale(300)).height(LayoutHelper.getInstance().scale(80))
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
        
        logger.info("LoginScreenUI screen shown");
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
    
    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        logger.info("Login attempt for username: {}", username);
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        // Disable UI while loading
        errorLabel.setVisible(false);
        
        authService.login(username, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(com.sweepgame.network.dto.AuthResponseDTO response) {
                logger.info("Login successful for user: {}", response.getUsername());
                // Start refresh scheduler on successful login
                authService.startTokenRefreshScheduler();
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
        logger.warn("Login error: {}", message);
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
        logger.info("LoginScreenUI disposed");
    }
}
