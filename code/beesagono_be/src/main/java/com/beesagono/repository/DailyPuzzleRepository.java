package com.beesagono.repository;

import com.beesagono.entity.DailyPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyPuzzleRepository extends JpaRepository<DailyPuzzle, String> {
    Optional<DailyPuzzle> findByPuzzleDate(LocalDate puzzleDate);
}