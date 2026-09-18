package com.beesagono.backend.service;

import com.beesagono.backend.dto.stats.PlayerStatsResponse;
import com.beesagono.backend.entity.PlayerStats;
import com.beesagono.backend.repository.PlayerStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerStatsServiceImpl implements PlayerStatsService {

    private final PlayerStatsRepository playerStatsRepository;

    @Override
    @Transactional(readOnly = true)
    public PlayerStatsResponse getPlayerStats(String userId) {
        PlayerStats stats = playerStatsRepository.findById(userId)
                .orElseGet(() -> PlayerStats.builder()
                        .userId(userId)
                        .gamesPlayed(0)
                        .gamesCompleted(0)
                        .currentStreak(0)
                        .maxStreak(0)
                        .totalScoreEarned(0)
                        .build());

        return mapToResponse(stats);
    }

    @Override
    @Transactional
    public void updatePlayerStatsAfterGame(String userId, int gameScore, String bestWordFound, boolean isCompleted) {
        PlayerStats stats = playerStatsRepository.findById(userId)
                .orElseGet(() -> PlayerStats.builder()
                        .userId(userId)
                        .gamesPlayed(0)
                        .gamesCompleted(0)
                        .currentStreak(0)
                        .maxStreak(0)
                        .totalScoreEarned(0)
                        .build());

        int gamesPlayed = (stats.getGamesPlayed() != null ? stats.getGamesPlayed() : 0) + 1;
        stats.setGamesPlayed(gamesPlayed);

        int currentTotalScore = stats.getTotalScoreEarned() != null ? stats.getTotalScoreEarned() : 0;
        stats.setTotalScoreEarned(currentTotalScore + gameScore);

        if (isCompleted) {
            stats.setGamesCompleted((stats.getGamesCompleted() != null ? stats.getGamesCompleted() : 0) + 1);
        }

        if (bestWordFound != null) {
            String currentLongest = stats.getLongestWordFound();
            if (currentLongest == null || bestWordFound.length() > currentLongest.length()) {
                stats.setLongestWordFound(bestWordFound);
            }
        }

        playerStatsRepository.save(stats);
    }

    private PlayerStatsResponse mapToResponse(PlayerStats stats) {
        int gamesPlayed = stats.getGamesPlayed() != null ? stats.getGamesPlayed() : 0;
        int gamesCompleted = stats.getGamesCompleted() != null ? stats.getGamesCompleted() : 0;
        int totalScore = stats.getTotalScoreEarned() != null ? stats.getTotalScoreEarned() : 0;

        double averageScore = gamesPlayed > 0 ? (double) totalScore / gamesPlayed : 0.0;
        double completionRate = gamesPlayed > 0 ? ((double) gamesCompleted / gamesPlayed) * 100.0 : 0.0;

        return PlayerStatsResponse.builder()
                .userId(stats.getUserId())
                .gamesPlayed(gamesPlayed)
                .gamesCompleted(gamesCompleted)
                .currentStreak(stats.getCurrentStreak() != null ? stats.getCurrentStreak() : 0)
                .maxStreak(stats.getMaxStreak() != null ? stats.getMaxStreak() : 0)
                .longestWordFound(stats.getLongestWordFound())
                .totalScoreEarned(totalScore)
                .averageScorePerGame(Math.round(averageScore * 100.0) / 100.0)
                .completionRate(Math.round(completionRate * 100.0) / 100.0)
                .build();
    }
}