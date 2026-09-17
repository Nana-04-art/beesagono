package com.beesagono.backend.controller;

import com.beesagono.backend.dto.stats.PlayerStatsResponse;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.PlayerStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Player Stats Controller", description = "Long-term user career statistics and personal records")
public class PlayerStatsController {

    private final PlayerStatsService playerStatsService;

    @Operation(summary = "Retrieve overall career statistics and personal records")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<PlayerStatsResponse> getMyStats(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(playerStatsService.getPlayerStats(userDetails.getId()));
    }
}