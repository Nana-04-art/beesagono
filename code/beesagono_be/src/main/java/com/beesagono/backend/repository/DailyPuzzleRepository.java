package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DailyPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPuzzleRepository extends JpaRepository<DailyPuzzle, String> {
    Optional<DailyPuzzle> findByPuzzleDate(LocalDate puzzleDate);

    boolean existsByPuzzleDate(LocalDate puzzleDate);

    List<DailyPuzzle> findAllByOrderByPuzzleDateDesc();
}