package com.beesagono.backend.service;

import com.beesagono.backend.dto.stats.PlayerStatsResponse;

/**
 * Service interface managing long-term career player statistics, streak counters,
 * and high-level milestones.
 */
public interface PlayerStatsService {

    /**
     * Retrieves aggregated career statistics for a specific player.
     *
     * @param userId unique identifier of the player
     * @return {@link PlayerStatsResponse} containing lifetime metrics, average scores, and streaks
     */
    PlayerStatsResponse getPlayerStats(String userId);

    /**
     * Updates long-term career statistics, daily streaks, and completion counts after a game session concludes.
     *
     * @param userId        unique identifier of the player
     * @param gameScore     total score achieved in the completed game
     * @param bestWordFound longest or highest-scoring word discovered during session
     * @param isCompleted   flag indicating 100% puzzle solution completion
     */
    void updatePlayerStatsAfterGame(String userId, int gameScore, String bestWordFound, boolean isCompleted);
}