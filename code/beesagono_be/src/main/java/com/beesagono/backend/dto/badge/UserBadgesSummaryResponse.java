package com.beesagono.backend.dto.badge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object representing the global badge summary for a user,
 * including total counters, overall completion percentage, and filtered badge
 * list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBadgesSummaryResponse {
    private int totalBadges;
    private int unlockedBadges;
    private double overallCompletionPercentage;
    private List<BadgeResponse> badges;
}