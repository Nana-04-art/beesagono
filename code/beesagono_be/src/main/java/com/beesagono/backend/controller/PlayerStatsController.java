package com.beesagono.backend.controller;

import com.beesagono.backend.dto.stats.PlayerStatsResponse;
import com.beesagono.backend.dto.stats.StatsSyncRequest;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.PlayerStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Player Stats Controller", description = "Endpoints for long-term user career statistics, game metrics, and personal records")
public class PlayerStatsController {

    private final PlayerStatsService playerStatsService;

    @Operation(summary = "Retrieve overall career statistics and personal records", description = "Fetches comprehensive long-term player metrics, including total games played, win streaks, points earned, and overall achievements.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Career statistics retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlayerStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Player statistics not found for the user", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error while retrieving career statistics", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<PlayerStatsResponse> getMyStats(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(playerStatsService.getPlayerStats(userDetails.getId()));
    }

    @Operation(summary = "Synchronize local offline game statistics with the server", description = "Merges local offline user statistics and metrics with the remote player profile stored on the server.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics synchronized successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlayerStatsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid synchronization request payload", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during statistics synchronization", content = @Content)
    })
    @PostMapping("/sync")
    public ResponseEntity<PlayerStatsResponse> syncLocalStats(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userPrincipal,
            @Valid @RequestBody StatsSyncRequest request) {

        // Use the UUID extracted from UserDetailsImpl instead of the username
        String userId = userPrincipal.getId();

        return ResponseEntity.ok(playerStatsService.syncLocalStatsWithServer(userId, request));
    }
}