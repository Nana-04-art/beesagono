package com.beesagono.backend.repository;

import com.beesagono.backend.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Directly retrieves the LocalDates of puzzles played by the user
    @Query("SELECT DISTINCT gs.puzzle.puzzleDate " +
            "FROM GameSession gs " +
            "WHERE gs.user.id = :userId " +
            "ORDER BY gs.puzzle.puzzleDate DESC")
    List<LocalDate> findDistinctPlayedPuzzleDatesByUserId(@Param("userId") String userId);

    Optional<GameSession> findFirstByUserIdOrderByStartTimeDesc(String userId);
}