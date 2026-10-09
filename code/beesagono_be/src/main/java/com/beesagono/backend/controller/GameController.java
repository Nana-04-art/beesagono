package com.beesagono.backend.controller;

import com.beesagono.backend.dto.game.GameBulkSyncRequest;
import com.beesagono.backend.dto.game.GameSessionResponse;
import com.beesagono.backend.dto.game.SubmitWordRequest;
import com.beesagono.backend.dto.game.SubmitWordResponse;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
@Tag(name = "Game Controller", description = "Endpoints for active gameplay management, word submissions, and offline progress synchronization")
public class GameController {

    private final GameService gameService;

    @Operation(summary = "Retrieve or initialize today's game session", description = "Fetches the current day's active puzzle session for the authenticated user or initializes a new session if one does not exist yet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game session retrieved or initialized successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameSessionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Daily puzzle not found for today's date", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error while retrieving session", content = @Content)
    })
    @GetMapping("/session/today")
    public ResponseEntity<GameSessionResponse> getTodaySession(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(gameService.getOrCreateTodaySession(userDetails.getId()));
    }

    @Operation(summary = "Submit and validate a played word", description = "Validates the user's submitted word against the active puzzle rules and updates user score and progress.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Word processed and scored successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubmitWordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or malformed word input", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied to the specified game session", content = @Content),
            @ApiResponse(responseCode = "404", description = "Game session not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during word validation", content = @Content)
    })
    @PostMapping("/submit")
    public ResponseEntity<SubmitWordResponse> submitWord(
            @Valid @RequestBody SubmitWordRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(gameService.validateAndScoreWord(request, userDetails.getId()));
    }

    @Operation(summary = "Synchronize local offline game progress with the server", description = "Performs bulk synchronization of offline gameplay progress and attempted words with the backend server.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Synchronization completed successfully", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = GameSessionResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid bulk synchronization payload", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during bulk progress synchronization", content = @Content)
    })
    @PostMapping("/sync")
    public ResponseEntity<List<GameSessionResponse>> syncLocalProgress(
            @Valid @RequestBody GameBulkSyncRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(gameService.syncLocalProgress(request, userDetails.getId()));
    }
}