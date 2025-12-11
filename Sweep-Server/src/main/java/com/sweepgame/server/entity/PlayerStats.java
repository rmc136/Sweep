package com.sweepgame.server.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "player_stats")
@Data
public class PlayerStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Integer gamesPlayed = 0;
    private Integer gamesWon = 0;
    private Integer totalPoints = 0;
    private Integer totalSweeps = 0;
    private Integer winStreak = 0;
    private Integer bestWinStreak = 0;
}
