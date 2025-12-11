package com.sweepgame.server.controller;

import com.sweepgame.server.model.dto.AuthResponseDTO;
import com.sweepgame.server.model.dto.LoginRequestDTO;
import com.sweepgame.server.model.dto.RegisterRequestDTO;
import com.sweepgame.server.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequestDTO request,
            @RequestHeader(value = "X-Platform", defaultValue = "browser") String platform) {
        logger.info("Registration request received for username: {} from platform: {}", 
                    request.getUsername(), platform);

        try {
            boolean isMobile = "mobile".equalsIgnoreCase(platform) || "desktop".equalsIgnoreCase(platform);
            Map<String, String> tokens = authService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                isMobile
            );

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", tokens.get("accessToken"));
            response.put("refreshToken", tokens.get("refreshToken"));
            response.put("username", request.getUsername());
            response.put("message", "User registered successfully");

            logger.info("User registered successfully: {}", request.getUsername());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            logger.error("Registration error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequestDTO request,
            @RequestHeader(value = "X-Platform", defaultValue = "browser") String platform) {
        logger.info("Login request received for username: {} from platform: {}", 
                    request.getUsername(), platform);

        try {
            boolean isMobile = "mobile".equalsIgnoreCase(platform) || "desktop".equalsIgnoreCase(platform);
            Map<String, String> tokens = authService.loginUser(
                request.getUsername(),
                request.getPassword(),
                isMobile
            );

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", tokens.get("accessToken"));
            response.put("refreshToken", tokens.get("refreshToken"));
            response.put("username", request.getUsername());
            response.put("message", "Login successful");

            logger.info("User logged in successfully: {}", request.getUsername());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Login failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (Exception e) {
            logger.error("Login error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        logger.info("Token refresh request received");

        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new IllegalArgumentException("Refresh token is required");
            }

            String newAccessToken = authService.refreshAccessToken(refreshToken);

            Map<String, String> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("message", "Token refreshed successfully");

            logger.info("Token refreshed successfully");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (Exception e) {
            logger.error("Token refresh error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
