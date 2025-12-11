package com.sweepgame.server.service;

import com.sweepgame.server.config.JwtConfig;
import com.sweepgame.server.entity.PlayerStats;
import com.sweepgame.server.entity.User;
import com.sweepgame.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtConfig jwtConfig;

    public Map<String, String> registerUser(String username, String email, String password, boolean isMobile) {
        logger.info("Attempting to register user: {}", username);

        if (username == null || username.trim().isEmpty()) {
            logger.warn("Registration failed: username is empty");
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (email == null || email.trim().isEmpty()) {
            logger.warn("Registration failed: email is empty");
            throw new IllegalArgumentException("Email cannot be empty");
        }

        if (password == null || password.length() < 6) {
            logger.warn("Registration failed: password too short");
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        if (userRepository.existsByUsername(username)) {
            logger.warn("Registration failed: username already exists: {}", username);
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            logger.warn("Registration failed: email already exists: {}", email);
            throw new IllegalArgumentException("Email already exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());

        PlayerStats stats = new PlayerStats();
        stats.setUser(user);
        user.setStats(stats);
        user = userRepository.save(user);
        logger.info("User registered successfully: {}", username);
        
        String accessToken = jwtConfig.generateAccessToken(username, user.getId());
        String refreshToken = jwtConfig.generateRefreshToken(username, user.getId(), isMobile);
        logger.debug("JWT tokens generated for user: {}", username);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    public Map<String, String> loginUser(String username, String password, boolean isMobile) {
        logger.info("Login attempt for user: {}", username);

        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            logger.warn("Login failed: user not found: {}", username);
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.warn("Login failed: incorrect password for user: {}", username);
            throw new IllegalArgumentException("Invalid username or password");
        }
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        String accessToken = jwtConfig.generateAccessToken(username, user.getId());
        String refreshToken = jwtConfig.generateRefreshToken(username, user.getId(), isMobile);
        logger.info("User logged in successfully: {}", username);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    public String refreshAccessToken(String refreshToken) {
        logger.info("Attempting to refresh access token");

        if (!jwtConfig.isRefreshToken(refreshToken)) {
            logger.warn("Invalid token type for refresh");
            throw new IllegalArgumentException("Invalid refresh token");
        }

        if (jwtConfig.isTokenExpired(refreshToken)) {
            logger.warn("Refresh token expired");
            throw new IllegalArgumentException("Refresh token expired");
        }

        String username = jwtConfig.extractUsername(refreshToken);
        Long userId = jwtConfig.extractUserId(refreshToken);

        String newAccessToken = jwtConfig.generateAccessToken(username, userId);
        logger.info("Access token refreshed for user: {}", username);

        return newAccessToken;
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public boolean validateToken(String token, String username) {
        return jwtConfig.validateToken(token, username);
    }
}
