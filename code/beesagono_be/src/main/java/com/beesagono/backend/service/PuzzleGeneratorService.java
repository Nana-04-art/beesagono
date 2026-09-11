package com.beesagono.backend.service;

import com.beesagono.backend.entity.DailyPuzzle;
import java.time.LocalDate;

public interface PuzzleGeneratorService {
    DailyPuzzle generateAndSavePuzzleForDate(LocalDate date);

    void recalculatePuzzleWords(DailyPuzzle puzzle);
}