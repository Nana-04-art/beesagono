package com.beesagono.backend.service;

import com.beesagono.backend.dto.stats.PlayerStatsResponse;
import com.beesagono.backend.dto.stats.StatsSyncRequest;

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
     * @param userId         unique identifier of the player
     * @param newPoints      total score achieved in the completed game
     * @param longestWord    longest or highest-scoring word discovered during session
     * @param isCompleted    flag indicating 100% puzzle solution completion
     * @param hasPlayedWords flag indicating whether at least one word was submitted during session
     */
    void updatePlayerStatsAfterGame(String userId, int newPoints, String longestWord, boolean isCompleted, boolean hasPlayedWords);

    /**
     * Synchronizes locally stored client game statistics with the server-side player profile.
     *
     * @param userId  unique identifier of the player
     * @param request payload containing offline game stats and progress metrics to merge
     * @return {@link PlayerStatsResponse} containing updated consolidated career statistics
     */
    PlayerStatsResponse syncLocalStatsWithServer(String userId, StatsSyncRequest request);
}