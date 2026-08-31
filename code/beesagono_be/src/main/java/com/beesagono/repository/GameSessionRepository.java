package com.beesagono.repository;

import com.beesagono.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, String> {
    Optional<GameSession> findByUserIdAndPuzzleId(String userId, String puzzleId);

    List<GameSession> findByUserId(String userId);
}