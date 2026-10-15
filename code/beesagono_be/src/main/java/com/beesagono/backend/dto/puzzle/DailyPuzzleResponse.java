package com.beesagono.backend.dto.puzzle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * Data Transfer Object representing the public configuration details of a daily puzzle for player clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyPuzzleResponse {

    private String id;
    private LocalDate puzzleDate;
    private String centerLetter;
    private Set<String> outerLetters;
    private Integer maxScore;
    private Integer totalWordsCount;        // Number of total words
    private Integer totalMielegrammiCount;  // Number of total mielegrammi

    // Letter statistics for each letter in the puzzle, including counts of found words and pangrams.
    private Map<String, Integer> wordsCountByLetter;        // E.g.: {"A": 5, "C": 3, ...}
    private Map<String, Integer> mielegrammiCountByLetter;  // E.g.: {"A": 1, "C": 0, ...}
}