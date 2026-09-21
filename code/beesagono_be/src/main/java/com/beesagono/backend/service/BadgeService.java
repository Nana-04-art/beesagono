package com.beesagono.backend.service;

import com.beesagono.backend.dto.badge.BadgeResponse;

import java.util.List;

public interface BadgeService {

    /**
     * Retrieves the complete list of badges indicating which ones are unlocked by
     * the user
     */
    List<BadgeResponse> getUserBadges(String userId);

    /**
     * Automatically evaluates and awards badges based on user actions/statistics
     */
    void evaluateAndAwardBadges(String userId);
}