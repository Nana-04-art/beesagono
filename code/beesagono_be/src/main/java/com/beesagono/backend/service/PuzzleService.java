package com.beesagono.backend.service;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;

/**
 * Service interface providing client-facing daily puzzle retrieval operations.
 */
public interface PuzzleService {

    /**
     * Retrieves today's active daily puzzle configuration for client rendering.
     *
     * @return {@link DailyPuzzleResponse} containing active puzzle letters and scoring rules
     */
    DailyPuzzleResponse getTodayPuzzle();
}