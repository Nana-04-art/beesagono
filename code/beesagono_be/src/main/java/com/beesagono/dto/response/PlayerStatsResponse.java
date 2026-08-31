package com.beesagono.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatsResponse {

    private String userId;
    private Integer currentStreak;
    private Integer maxStreak;
    private Integer totalPoints;
    private Integer gamesPlayed;
    private Integer gamesCompleted;
    private LocalDate lastPlayedDate;
}