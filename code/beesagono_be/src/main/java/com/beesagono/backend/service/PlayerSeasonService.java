package com.beesagono.backend.service;

import com.beesagono.backend.dto.stats.LeaderboardEntryDto;
import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.dto.stats.RankDistributionResponse;

import java.util.List;

public interface PlayerSeasonService {

    // Retrieves the current season statistics (current year) for the specified
    // user.

    PlayerSeasonResponse getCurrentSeasonStats(String userId);

    // Updates the current season points and status upon game progress or
    // completion.
    void updateSeasonProgress(String userId, int pointsEarned, boolean isGameCompleted);

    List<PlayerSeasonResponse> getPlayerSeasonHistory(String userId);

    RankDistributionResponse getRankDistribution();

    List<LeaderboardEntryDto> getTopLeaderboard(int limit);
}