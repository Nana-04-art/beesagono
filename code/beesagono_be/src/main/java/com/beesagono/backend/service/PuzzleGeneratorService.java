package com.beesagono.backend.service;

import java.time.LocalDate;

public interface PuzzleGeneratorService {
    void generateAndSavePuzzleForDate(LocalDate date);
}