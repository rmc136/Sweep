package com.sweepgame.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_history")
@Data
public class GameHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;

    @ManyToOne
    private User player1;

    @ManyToOne
    private User player2;

    @ManyToOne
    private User player3;

    @ManyToOne
    private User winner;

    private Integer player1Points;
    private Integer player2Points;
    private Integer player3Points;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer durationSeconds;
}
