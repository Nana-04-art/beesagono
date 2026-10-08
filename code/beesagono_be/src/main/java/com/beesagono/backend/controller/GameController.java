package com.beesagono.backend.controller;

import com.beesagono.backend.dto.game.GameBulkSyncRequest;
import com.beesagono.backend.dto.game.GameSessionResponse;
import com.beesagono.backend.dto.game.SubmitWordRequest;
import com.beesagono.backend.dto.game.SubmitWordResponse;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/session/today")
    public ResponseEntity<GameSessionResponse> getTodaySession(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        GameSessionResponse response = gameService.getOrCreateTodaySession(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/submit")
    public ResponseEntity<SubmitWordResponse> submitWord(
            @Valid @RequestBody SubmitWordRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        SubmitWordResponse response = gameService.validateAndScoreWord(request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync")
    public ResponseEntity<List<GameSessionResponse>> syncLocalProgress(
            @Valid @RequestBody GameBulkSyncRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        List<GameSessionResponse> response = gameService.syncLocalProgress(request, currentUser.getId());
        return ResponseEntity.ok(response);
    }
}