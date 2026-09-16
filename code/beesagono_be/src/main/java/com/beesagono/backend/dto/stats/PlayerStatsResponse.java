package com.beesagono.backend.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatsResponse {
    private String userId;
    private int gamesPlayed;
    private int gamesCompleted;
    private int currentStreak;
    private int maxStreak;
    private String longestWordFound;
    private int totalScoreEarned;
    private double averageScorePerGame; // Calculated: totalScoreEarned / gamesPlayed
    private double completionRate;      // Calculated: (gamesCompleted / gamesPlayed) * 100
}