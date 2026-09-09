package com.beesagono.backend.service;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.dto.puzzle.WordSubmissionResponse;

public interface PuzzleService {
    DailyPuzzleResponse getTodayPuzzle();

    WordSubmissionResponse validateAndScoreWord(String puzzleId, String word);
}