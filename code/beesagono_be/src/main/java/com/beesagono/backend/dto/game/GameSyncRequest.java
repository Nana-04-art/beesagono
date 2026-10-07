package com.beesagono.backend.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Data Transfer Object containing state details for syncing an individual game
 * session with the backend.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSyncRequest {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate puzzleDate;

    private String centerLetter;
    private List<String> outerLetters;
    private List<String> foundWords;
    private List<String> invalidWords;
    private List<String> foundMielegrammi;
    private Integer currentScore;
    private String currentRankLabel;
    private Long startTime;
    private Long lastUpdated;
    private Double completionPercentage; // E.g. 0.25 for 25% completion
    private Boolean isCompleted;
}