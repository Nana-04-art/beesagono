package com.beesagono.backend.service;

import com.beesagono.backend.dto.stats.PlayerStatsResponse;

public interface PlayerStatsService {
    PlayerStatsResponse getPlayerStats(String userId);

    void updatePlayerStatsAfterGame(String userId, int gameScore, String bestWordFound, boolean isCompleted);
}