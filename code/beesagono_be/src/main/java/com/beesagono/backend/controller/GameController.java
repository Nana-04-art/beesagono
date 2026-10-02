package com.beesagono.backend.controller;

import com.beesagono.backend.dto.game.GameBulkSyncRequest;
import com.beesagono.backend.dto.game.GameSessionResponse;
import com.beesagono.backend.dto.game.SubmitWordRequest;
import com.beesagono.backend.dto.game.SubmitWordResponse;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
@Tag(name = "Game Controller", description = "Endpoints for gameplay management, word submission, and progress sync")
public class GameController {

    private final GameService gameService;

    @Operation(summary = "Retrieve or initialize today's game session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game session retrieved or initialized successfully"),
            @ApiResponse(responseCode = "404", description = "Daily puzzle not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/session/today")
    public ResponseEntity<GameSessionResponse> getTodaySession(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(gameService.getOrCreateTodaySession(userDetails.getId()));
    }

    @Operation(summary = "Submit and validate a played word")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Word processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Game session not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/submit")
    public ResponseEntity<SubmitWordResponse> submitWord(
            @Valid @RequestBody SubmitWordRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(gameService.validateAndScoreWord(request, userDetails.getId()));
    }

    @Operation(summary = "Synchronize local offline game progress with the server")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Synchronization completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid synchronization payload"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/sync")
    public ResponseEntity<List<GameSessionResponse>> syncLocalProgress(
            @Valid @RequestBody GameBulkSyncRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(gameService.syncLocalProgress(request, userDetails.getId()));
    }
}