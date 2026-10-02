package com.beesagono.backend.dto.puzzle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
}