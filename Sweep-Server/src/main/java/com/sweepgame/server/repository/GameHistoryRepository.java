package com.sweepgame.server.repository;

import com.sweepgame.server.entity.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
    
    @Query("SELECT g FROM GameHistory g WHERE g.player1.id = :userId OR g.player2.id = :userId OR g.player3.id = :userId ORDER BY g.finishedAt DESC")
    List<GameHistory> findAllGamesByUserId(@Param("userId") Long userId);
    
    List<GameHistory> findByWinnerIdOrderByFinishedAtDesc(Long winnerId);
}
