package com.sweepgame.server.controller;

import com.sweepgame.server.config.JwtConfig;
import com.sweepgame.server.entity.GameHistory;
import com.sweepgame.server.entity.PlayerStats;
import com.sweepgame.server.entity.User;
import com.sweepgame.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtConfig jwtConfig;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        logger.info("Profile request from user: {}", authentication.getName());

        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
            }

            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("emailVerified", user.isEmailVerified());
            profile.put("provider", user.getProvider());
            profile.put("createdAt", user.getCreatedAt());
            profile.put("lastLogin", user.getLastLogin());

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            logger.error("Error fetching profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestBody Map<String, String> updates) {
        logger.info("Profile update request from user: {}", authentication.getName());

        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
            }

            String newUsername = updates.get("username");
            String newEmail = updates.get("email");

            User updatedUser = userService.updateProfile(user.getId(), newUsername, newEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("username", updatedUser.getUsername());
            response.put("email", updatedUser.getEmail());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Profile update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            logger.error("Error updating profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(Authentication authentication) {
        logger.info("Stats request from user: {}", authentication.getName());

        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
            }

            PlayerStats stats = userService.getUserStats(user.getId());
            if (stats == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Stats not found"));
            }

            Map<String, Object> statsMap = new HashMap<>();
            statsMap.put("gamesPlayed", stats.getGamesPlayed());
            statsMap.put("gamesWon", stats.getGamesWon());
            statsMap.put("totalPoints", stats.getTotalPoints());
            statsMap.put("totalSweeps", stats.getTotalSweeps());
            statsMap.put("winStreak", stats.getWinStreak());
            statsMap.put("bestWinStreak", stats.getBestWinStreak());
            statsMap.put("rankedPoints", stats.getRankedPoints());

            return ResponseEntity.ok(statsMap);

        } catch (Exception e) {
            logger.error("Error fetching stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(Authentication authentication) {
        logger.info("Game history request from user: {}", authentication.getName());

        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
            }

            List<GameHistory> history = userService.getUserGameHistory(user.getId());

            return ResponseEntity.ok(Map.of("history", history));

        } catch (Exception e) {
            logger.error("Error fetching game history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/history/wins")
    public ResponseEntity<?> getHistoryWins(Authentication authentication) {
        logger.info("Game history wins request from user: {}", authentication.getName());

        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
            }

            List<GameHistory> history = userService.getUserGameHistoryWins(user.getId());

            return ResponseEntity.ok(Map.of("history", history));

        } catch (Exception e) {
            logger.error("Error fetching game history wins", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAccount(Authentication authentication) {
        logger.info("Account deletion request from user: {}", authentication.getName());

        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
            }

            userService.deleteUser(user.getId());

            return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));

        } catch (Exception e) {
            logger.error("Error deleting account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }
}
