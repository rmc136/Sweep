package com.sweepgame.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    // Email verification fields (for future implementation)
    @Column(nullable = false)
    private boolean emailVerified = false;
    
    private String verificationToken;
    private LocalDateTime verificationTokenExpiry;

    // OAuth2 fields (for future Google login)
    private String googleId;
    
    @Column(nullable = false)
    private String provider = "local"; // "local", "google", etc.

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private PlayerStats stats;
}
