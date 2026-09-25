package com.beesagono.backend.controller;

import com.beesagono.backend.dto.badge.BadgeResponse;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.BadgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
@Tag(name = "Badge Controller", description = "Endpoints for user badge management and achievement progress tracking")
public class BadgeController {

    private final BadgeService badgeService;

    @Operation(summary = "Retrieve user badges with progress details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Badges and progression retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    /**
     * Retrieves all user badges and progress.
     * Triggers an evaluation first to guarantee the response reflects any newly
     * unlocked badges.
     */
    @GetMapping
    public ResponseEntity<List<BadgeResponse>> getUserBadges(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Evaluates and awards any newly earned badges before returning the list
        badgeService.evaluateAndAwardBadges(userDetails.getId());
        return ResponseEntity.ok(badgeService.getUserBadges(userDetails.getId()));
    }

    @Operation(summary = "Trigger manual evaluation and awarding of user badges")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Badges evaluated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    /**
     * Action-only endpoint to explicitly trigger a badge evaluation (e.g. after a
     * game event).
     * Does not return payload data to minimize network overhead.
     */
    @PostMapping("/evaluate")
    public ResponseEntity<Void> evaluateBadges(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        badgeService.evaluateAndAwardBadges(userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}