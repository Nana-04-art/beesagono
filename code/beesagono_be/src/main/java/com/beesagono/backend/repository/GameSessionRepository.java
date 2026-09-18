package com.beesagono.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beesagono.backend.entity.GameSession;

public interface GameSessionRepository extends JpaRepository<GameSession, String> {

    Optional<GameSession> findByUserIdAndPuzzleId(String userId, String puzzleId);

    List<GameSession> findByUserId(String userId);

    @Query("SELECT COUNT(gs) > 0 FROM GameSession gs JOIN gs.foundWords fw WHERE gs.user.id = :userId AND gs.puzzle.id = :puzzleId AND fw.id.word = :word")
    boolean existsByUserIdAndPuzzleIdAndFoundWordsContaining(
            @Param("userId") String userId,
            @Param("puzzleId") String puzzleId,
            @Param("word") String word);
}