package com.beesagono.backend.controller;

import com.beesagono.backend.dto.stats.LeaderboardEntryDto;
import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.dto.stats.RankDistributionResponse;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.PlayerSeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seasons")
@RequiredArgsConstructor
public class PlayerSeasonController {

    private final PlayerSeasonService playerSeasonService;

    @GetMapping("/me")
    public ResponseEntity<PlayerSeasonResponse> getMySeasonStats(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        PlayerSeasonResponse response = playerSeasonService.getCurrentSeasonStats(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<PlayerSeasonResponse>> getMySeasonHistory(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        List<PlayerSeasonResponse> history = playerSeasonService.getPlayerSeasonHistory(userDetails.getId());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/rank-distribution")
    public ResponseEntity<RankDistributionResponse> getRankDistribution() {
        return ResponseEntity.ok(playerSeasonService.getRankDistribution());
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(playerSeasonService.getTopLeaderboard(limit));
    }
}