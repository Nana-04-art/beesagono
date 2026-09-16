package com.beesagono.backend.controller;

import com.beesagono.backend.dto.stats.PlayerStatsResponse;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.PlayerStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class PlayerStatsController {

    private final PlayerStatsService playerStatsService;

    @GetMapping("/me")
    public ResponseEntity<PlayerStatsResponse> getMyStats(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        PlayerStatsResponse response = playerStatsService.getPlayerStats(userDetails.getId());
        return ResponseEntity.ok(response);
    }

}