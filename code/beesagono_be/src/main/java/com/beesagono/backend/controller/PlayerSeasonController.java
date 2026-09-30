package com.beesagono.backend.controller;

import com.beesagono.backend.dto.stats.LeaderboardEntryDto;
import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.dto.stats.RankDistributionResponse;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.PlayerSeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Player Season Controller", description = "Endpoints for seasonal player metrics, rank progression, rank distribution, and global leaderboards")
public class PlayerSeasonController {

    private final PlayerSeasonService playerSeasonService;

    @Operation(summary = "Get current season statistics for the authenticated user", description = "Retrieves seasonal performance metrics, rank status, and points earned during the active season for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current season statistics retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlayerSeasonResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "404", description = "No active season found for the user", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error while retrieving current season stats", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<PlayerSeasonResponse> getMySeasonStats(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(playerSeasonService.getCurrentSeasonStats(userDetails.getId()));
    }

    @Operation(summary = "Get user performance history from past seasons", description = "Returns a historical list of seasonal performances, ranks, and total points accumulated across previous completed seasons.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Player season history retrieved successfully", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PlayerSeasonResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error while retrieving season history", content = @Content)
    })
    @GetMapping("/history")
    public ResponseEntity<List<PlayerSeasonResponse>> getMySeasonHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(playerSeasonService.getPlayerSeasonHistory(userDetails.getId()));
    }

    @Operation(summary = "Get global player rank distribution", description = "Provides statistical distribution showing the percentage and count of players across all active competitive ranks.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rank distribution statistics retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RankDistributionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error while calculating rank distribution", content = @Content)
    })
    @GetMapping("/rank-distribution")
    public ResponseEntity<RankDistributionResponse> getRankDistribution() {
        return ResponseEntity.ok(playerSeasonService.getRankDistribution());
    }

    @Operation(summary = "Get top player leaderboard for current season", description = "Retrieves the global leaderboard ranking the top performing players for the active season.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leaderboard entries retrieved successfully", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = LeaderboardEntryDto.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid limit parameter", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error while retrieving leaderboard", content = @Content)
    })
    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @Parameter(description = "Maximum number of leaderboard positions to return", example = "10") @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(playerSeasonService.getTopLeaderboard(limit));
    }
}