package com.beesagono.backend.controller;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.service.PuzzleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/puzzles")
@RequiredArgsConstructor
@Tag(name = "Puzzle Controller", description = "Public endpoints for retrieving daily puzzle data")
public class PuzzleController {

    private final PuzzleService puzzleService;

    @Operation(summary = "Retrieve today's daily puzzle", description = "Returns the active daily puzzle configuration including outer letters, center letter, and scoring parameters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Today's puzzle retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DailyPuzzleResponse.class))),
            @ApiResponse(responseCode = "404", description = "No active puzzle found for today's date", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error while retrieving daily puzzle", content = @Content)
    })
    @GetMapping("/today")
    public ResponseEntity<DailyPuzzleResponse> getTodayPuzzle() {
        return ResponseEntity.ok(puzzleService.getTodayPuzzle());
    }
}