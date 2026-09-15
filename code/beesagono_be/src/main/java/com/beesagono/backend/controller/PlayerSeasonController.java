package com.beesagono.backend.controller;

import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.PlayerSeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class PlayerSeasonController {

    private final PlayerSeasonService playerSeasonService;

    @GetMapping("/me")
    public ResponseEntity<PlayerSeasonResponse> getMySeasonStats(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        PlayerSeasonResponse response = playerSeasonService.getCurrentSeasonStats(userDetails.getId());
        return ResponseEntity.ok(response);
    }
}