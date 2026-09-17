package com.beesagono.backend.service;

import com.beesagono.backend.dto.stats.LeaderboardEntryDto;
import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.dto.stats.RankDistributionResponse;

import java.util.List;

/**
 * Service interface for tracking seasonal player performance, progression history,
 * tier counts, and global leaderboards.
 */
public interface PlayerSeasonService {

    /**
     * Retrieves current active season statistics for a specific player.
     *
     * @param userId unique identifier of the player
     * @return {@link PlayerSeasonResponse} containing score totals and tier standing
     */
    PlayerSeasonResponse getCurrentSeasonStats(String userId);

    /**
     * Updates seasonal point accumulators and checks tier promotion eligibility after gameplay progress.
     *
     * @param userId          unique identifier of the player
     * @param pointsEarned    base points earned during current session
     * @param isGameCompleted flag indicating whether the daily puzzle was fully solved
     */
    void updateSeasonProgress(String userId, int pointsEarned, boolean isGameCompleted);

    /**
     * Retrieves historical seasonal records across all past years for a specific player.
     *
     * @param userId unique identifier of the player
     * @return list of {@link PlayerSeasonResponse} records sorted by season year
     */
    List<PlayerSeasonResponse> getPlayerSeasonHistory(String userId);

    /**
     * Aggregates current seasonal player counts grouped by career tier ranks.
     *
     * @return {@link RankDistributionResponse} containing total player counts per tier
     */
    RankDistributionResponse getRankDistribution();

    /**
     * Retrieves top-ranking players for the active season ordered by total points.
     *
     * @param limit maximum number of top leaderboard entries to return
     * @return list of {@link LeaderboardEntryDto} objects
     */
    List<LeaderboardEntryDto> getTopLeaderboard(int limit);
}