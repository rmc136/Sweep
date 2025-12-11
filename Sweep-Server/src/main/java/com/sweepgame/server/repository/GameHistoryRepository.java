package com.sweepgame.server.repository;

import com.sweepgame.server.entity.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {

    // TODO: Add custom query methods
}
