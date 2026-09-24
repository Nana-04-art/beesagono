package com.beesagono.backend.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing an individual entry within the seasonal leaderboard,
 * containing player identity, total score, highest career tier, and current rank position.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDto {
    private String username;
    private int totalPoints;
    private String highestTierAchieved;
    private int rankPosition;
}