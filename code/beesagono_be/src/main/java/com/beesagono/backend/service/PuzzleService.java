package com.beesagono.backend.service;

import java.time.LocalDate;
import java.util.List;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;

public interface PuzzleService {
    DailyPuzzleResponse getTodayPuzzle();

    /**
     * Returns the list of valid words associated with the puzzle for the
     * specified date.
     * 
     * @param date the date of the puzzle
     * @return the list of valid words for that puzzle
     */
    List<String> getValidWordsByDate(LocalDate date);
}