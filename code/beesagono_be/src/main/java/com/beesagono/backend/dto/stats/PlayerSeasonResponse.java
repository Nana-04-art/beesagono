package com.beesagono.backend.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a player's performance breakdown for a specific season,
 * detailing base points, bonus points, combined score, and current tier status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSeasonResponse {

    private Integer year; // e.g. 2026
    private String highestTierAchieved; // e.g. "Uovo d'Ape"

    // Season Points
    private Integer basePoints; // Total points earned from words
    private Integer bonusPoints; // Bonus points from streak milestones
    private Integer totalPoints; // basePoints + bonusPoints
}