package com.sweepgame.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.google.gson.Gson;
import com.sweepgame.network.dto.AuthResponseDTO;
import com.sweepgame.network.dto.LoginRequestDTO;
import com.sweepgame.network.dto.RegisterRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String BASE_URL = "http://localhost:8080/api/auth";
    private static final String PREFS_NAME = "SweepAuth";
    
    private final Gson gson;
    private Preferences prefs; // REMOVED 'final' here
    
    public interface AuthCallback {
        void onSuccess(AuthResponseDTO response);
        void onError(String message);
    }
    
    public AuthService() {
        this.gson = new Gson();
        // Don't initialize prefs here - wait until it's needed
    }
    
    private Preferences getPrefs() {
        if (prefs == null) {
            prefs = Gdx.app.getPreferences(PREFS_NAME);
        }
        return prefs;
    }
    
    public void refreshToken(final AuthCallback callback) {
        String refreshToken = getPrefs().getString("refreshToken", null);
        if (refreshToken == null) {
            callback.onError("No refresh token available");
            return;
        }

        String json = "{\"refreshToken\":\"" + refreshToken + "\"}";
        sendRequest("/refresh", json, callback);
    }

    public void login(String username, String password, final AuthCallback callback) {
        LoginRequestDTO requestDTO = new LoginRequestDTO(username, password);
        String json = gson.toJson(requestDTO);
        
        sendRequest("/login", json, callback);
    }
    
    public void register(String username, String email, String password, final AuthCallback callback) {
        RegisterRequestDTO requestDTO = new RegisterRequestDTO(username, email, password);
        String json = gson.toJson(requestDTO);
        
        sendRequest("/register", json, callback);
    }
    
    private void sendRequest(String endpoint, String jsonContent, final AuthCallback callback) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
            .method(Net.HttpMethods.POST)
            .url(BASE_URL + endpoint)
            .header("Content-Type", "application/json")
            .content(jsonContent)
            .build();
            
        logger.info("Sending request to {}", httpRequest.getUrl());
        
        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String result = httpResponse.getResultAsString();
                int statusCode = httpResponse.getStatus().getStatusCode();
                
                logger.info("Response code: {}", statusCode);
                
                if (statusCode == 200) {
                    try {
                        AuthResponseDTO response = gson.fromJson(result, AuthResponseDTO.class);
                        saveSession(response);
                        Gdx.app.postRunnable(() -> callback.onSuccess(response));
                    } catch (Exception e) {
                        logger.error("Failed to parse response", e);
                        Gdx.app.postRunnable(() -> callback.onError("Invalid server response"));
                    }
                } else {
                    Gdx.app.postRunnable(() -> callback.onError("Request failed: " + statusCode));
                }
            }
            
            @Override
            public void failed(Throwable t) {
                logger.error("Network request failed", t);
                Gdx.app.postRunnable(() -> callback.onError("Network error: " + t.getMessage()));
            }
            
            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }
    
    private com.badlogic.gdx.utils.Timer.Task refreshTask;
    private static final float REFRESH_INTERVAL_SECONDS = 14 * 60; // 14 minutes

    public void startTokenRefreshScheduler() {
        stopTokenRefreshScheduler();
        
        refreshTask = new com.badlogic.gdx.utils.Timer.Task() {
            @Override
            public void run() {
                logger.info("Executing scheduled token refresh...");
                refreshToken(new AuthCallback() {
                    @Override
                    public void onSuccess(AuthResponseDTO response) {
                        logger.info("Scheduled token refresh successful");
                    }
                    
                    @Override
                    public void onError(String message) {
                        logger.error("Scheduled token refresh failed: {}", message);
                        stopTokenRefreshScheduler();
                    }
                });
            }
        };
        
        com.badlogic.gdx.utils.Timer.schedule(refreshTask, REFRESH_INTERVAL_SECONDS, REFRESH_INTERVAL_SECONDS);
        logger.info("Token refresh scheduler started (interval: {}s)", REFRESH_INTERVAL_SECONDS);
    }
    
    public void stopTokenRefreshScheduler() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
            logger.info("Token refresh scheduler stopped");
        }
    }
    
    private void saveSession(AuthResponseDTO response) {
        Preferences p = getPrefs(); // Use getPrefs() here too
        if (response.getAccessToken() != null) {
            p.putString("accessToken", response.getAccessToken());
        }
        if (response.getRefreshToken() != null) {
            p.putString("refreshToken", response.getRefreshToken());
        }
        if (response.getUsername() != null) {
            p.putString("username", response.getUsername());
        }
        p.flush();
        logger.info("Session updated");
    }
    
    public String getAccessToken() {
        return getPrefs().getString("accessToken", null);
    }
    
    public String getUsername() {
        return getPrefs().getString("username", null);
    }
    
    public void logout() {
        stopTokenRefreshScheduler();
        Preferences p = getPrefs();
        p.clear();
        p.flush();
    }
    
    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }
}