package com.beesagono.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private LocalDateTime startTime;
    private LocalDateTime lastUpdated;
}
