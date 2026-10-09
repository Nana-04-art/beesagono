package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DailyPuzzle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPuzzleRepository extends JpaRepository<DailyPuzzle, String> {
    Optional<DailyPuzzle> findByPuzzleDate(LocalDate puzzleDate);

    boolean existsByPuzzleDate(LocalDate puzzleDate);

    List<DailyPuzzle> findAllByOrderByPuzzleDateDesc(Pageable pageable);

    /**
     * Retrieves all puzzles with a date later than the specified one (future
     * puzzles), eagerly loading the puzzleWords and their associated dictionaryWords to avoid
     * LazyInitializationException.
     */
    @EntityGraph(attributePaths = { "puzzleWords", "puzzleWords.dictionaryWord" })
    List<DailyPuzzle> findAllByPuzzleDateAfter(LocalDate date);
}