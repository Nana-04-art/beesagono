package com.beesagono.backend.service;

import com.beesagono.backend.entity.DailyPuzzle;
import java.time.LocalDate;

/**
 * Service interface responsible for procedural daily puzzle generation, letter selection,
 * and solution set calculations.
 */
public interface PuzzleGeneratorService {

    /**
     * Generates and persists a new valid daily puzzle for a specific scheduled date.
     *
     * @param date target schedule date
     * @return saved {@link DailyPuzzle} entity
     */
    DailyPuzzle generateAndSavePuzzleForDate(LocalDate date);

    /**
     * Recalculates and replaces the set of valid solution words for an existing daily puzzle instance.
     *
     * @param puzzle the target daily puzzle entity to update
     */
    void recalculatePuzzleWords(DailyPuzzle puzzle);
}