package com.beesagono.backend.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;

/**
 * Data Transfer Object representing detailed state information for a player's
 * current or past game session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSessionResponse {

    private String id;
    private String puzzleId;
    private String userId;
    private Integer currentScore;
    private String currentRankLabel;
    private Boolean isCompleted;
    private Set<String> foundWords;
    private Set<String> invalidWords;
    private Set<String> foundMielegrammi;
    private Date startTime;
    private Date lastUpdated;
}