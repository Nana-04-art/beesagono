package com.beesagono.backend.service;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;

public interface PuzzleService {
    DailyPuzzleResponse getTodayPuzzle();
}