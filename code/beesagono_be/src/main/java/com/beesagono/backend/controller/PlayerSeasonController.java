package com.beesagono.backend.controller;

import com.beesagono.backend.dto.stats.LeaderboardEntryDto;
import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.dto.stats.RankDistributionResponse;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.PlayerSeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Player Season Controller", description = "Seasonal metrics, rank history, and global leaderboards")
public class PlayerSeasonController {

    private final PlayerSeasonService playerSeasonService;

    @Operation(summary = "Get current season statistics for the authenticated user")
    @GetMapping("/me")
    public ResponseEntity<PlayerSeasonResponse> getMySeasonStats(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(playerSeasonService.getCurrentSeasonStats(userDetails.getId()));
    }

    @Operation(summary = "Get user performance history from past seasons")
    @GetMapping("/history")
    public ResponseEntity<List<PlayerSeasonResponse>> getMySeasonHistory(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(playerSeasonService.getPlayerSeasonHistory(userDetails.getId()));
    }

    @Operation(summary = "Get global player rank distribution")
    @GetMapping("/rank-distribution")
    public ResponseEntity<RankDistributionResponse> getRankDistribution() {
        return ResponseEntity.ok(playerSeasonService.getRankDistribution());
    }

    @Operation(summary = "Get top player leaderboard for current season")
    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(playerSeasonService.getTopLeaderboard(limit));
    }
}