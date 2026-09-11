package com.beesagono.backend.repository;

import com.beesagono.backend.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, String> {

    Optional<GameSession> findByUserIdAndPuzzleId(String userId, String puzzleId);

    List<GameSession> findByUserId(String userId);

    // JPA navigates the relationship: user.id and puzzle.puzzleDate
    Optional<GameSession> findByUserIdAndPuzzlePuzzleDate(String userId, LocalDate puzzleDate);

    boolean existsByUserIdAndPuzzleId(String userId, String puzzleId);

    Long countByPuzzleIdAndIsCompletedFalse(String puzzleId);

    Long countByPuzzleIdAndIsCompletedTrue(String puzzleId);
}