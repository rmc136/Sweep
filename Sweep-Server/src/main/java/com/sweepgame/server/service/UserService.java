package com.sweepgame.server.service;

import com.sweepgame.server.entity.GameHistory;
import com.sweepgame.server.entity.PlayerStats;
import com.sweepgame.server.entity.User;
import com.sweepgame.server.repository.GameHistoryRepository;
import com.sweepgame.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameHistoryRepository gameHistoryRepository;

    public User getUserById(Long userId) {
        logger.debug("Fetching user by ID: {}", userId);
        return userRepository.findById(userId).orElse(null);
    }

    public User getUserByUsername(String username) {
        logger.debug("Fetching user by username: {}", username);
        return userRepository.findByUsername(username).orElse(null);
    }

    public User updateProfile(Long userId, String newUsername, String newEmail) {
        logger.info("Updating profile for user ID: {}", userId);

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.warn("User not found: {}", userId);
            throw new IllegalArgumentException("User not found");
        }

        User user = userOptional.get();

        if (newUsername != null && !newUsername.equals(user.getUsername())) {
            if (userRepository.existsByUsername(newUsername)) {
                logger.warn("Username already exists: {}", newUsername);
                throw new IllegalArgumentException("Username already exists");
            }
            user.setUsername(newUsername);
        }

        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            if (userRepository.existsByEmail(newEmail)) {
                logger.warn("Email already exists: {}", newEmail);
                throw new IllegalArgumentException("Email already exists");
            }
            user.setEmail(newEmail);
            user.setEmailVerified(false); // Need to verify new email
        }

        user = userRepository.save(user);
        logger.info("Profile updated successfully for user: {}", user.getUsername());
        return user;
    }

    public PlayerStats getUserStats(Long userId) {
        logger.debug("Fetching stats for user ID: {}", userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.warn("User not found: {}", userId);
            return null;
        }

        return userOptional.get().getStats();
    }

    public List<GameHistory> getUserGameHistory(Long userId) {
        logger.debug("Fetching game history for user ID: {}", userId);
        
        return gameHistoryRepository.findAllGamesByUserId(userId);
    }

    public List<GameHistory> getUserGameHistoryWins(Long userId) {
        logger.debug("Fetching game history wins for user ID: {}", userId);
        
        return gameHistoryRepository.findByWinnerIdOrderByFinishedAtDesc(userId);
    }

    public void deleteUser(Long userId) {
        logger.info("Deleting user ID: {}", userId);
        
        if (!userRepository.existsById(userId)) {
            logger.warn("User not found: {}", userId);
            throw new IllegalArgumentException("User not found");
        }

        userRepository.deleteById(userId);
        logger.info("User deleted successfully: {}", userId);
    }
}
