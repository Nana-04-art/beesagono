package com.beesagono.backend.service;

import com.beesagono.backend.dto.badge.BadgeResponse;
import com.beesagono.backend.dto.badge.UserBadgesSummaryResponse;

import java.util.List;

/**
 * Service interface for managing user badges, tracking achievement progression,
 * and evaluating unlocking criteria.
 */
public interface BadgeService {

    /**
     * Retrieves the complete list of badges indicating which ones are unlocked by
     * the user
     */
    List<BadgeResponse> getUserBadges(String userId);

    /**
     * Retrieves a summarized breakdown of user badges, supporting filtering by
     * category or unlock status, alongside overall completion statistics
     */
    UserBadgesSummaryResponse getUserBadgesSummary(String userId, String category, Boolean unlockedOnly);

    /**
     * Automatically evaluates and awards badges based on user actions/statistics
     */
    void evaluateAndAwardBadges(String userId);
}