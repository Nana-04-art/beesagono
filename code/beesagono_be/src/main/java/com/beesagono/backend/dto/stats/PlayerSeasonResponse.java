package com.beesagono.backend.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSeasonResponse {

    private Integer year; // e.g. 2026
    private String highestTierAchieved; // e.g. "Uovo d'Ape" (Career Level)

    // Yearly / Career KPIs
    private Integer gamesPlayed; // Games played during the year
    private Integer gamesCompleted; // Games completed during the year
    private Integer currentStreak; // Current streak (days)
    private Integer maxStreak; // Maximum streak record (days)

    // Season Points
    private Integer basePoints; // Total points earned from words
    private Integer bonusPoints; // Bonus points from streak milestones
    private Integer totalPoints; // basePoints + bonusPoints
}