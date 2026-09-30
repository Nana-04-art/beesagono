package com.beesagono.backend.controller;

import com.beesagono.backend.dto.puzzle.PuzzleAdminResponse;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleLettersRequest;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleWordsRequest;
import com.beesagono.backend.service.AdminService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/puzzle")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Puzzle Controller", description = "Administrative endpoints for generating, inspecting, and modifying puzzles")
public class AdminPuzzleController {

    private final AdminService adminService;

    @Operation(summary = "Generate or reset puzzle for a future date", description = "Allows an administrator to generate a new puzzle or reset an existing scheduled puzzle for a specific date.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Puzzle generated or reset successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PuzzleAdminResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date or date violates system rules", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized user", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during puzzle generation", content = @Content)
    })
    @PostMapping("/generate")
    public ResponseEntity<PuzzleAdminResponse> generatePuzzle(
            @Parameter(description = "Date to generate the puzzle for (Format: YYYY-MM-DD)", required = true, example = "2026-10-15") @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminService.generateOrResetFuturePuzzle(date));
    }

    @Operation(summary = "Get administrative puzzle details by date", description = "Retrieves full details (letters, valid words, maximum score) for the puzzle associated with the given date.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Puzzle details retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PuzzleAdminResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date format", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized user", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required", content = @Content),
            @ApiResponse(responseCode = "404", description = "No puzzle found for the specified date", content = @Content)
    })
    @GetMapping
    public ResponseEntity<PuzzleAdminResponse> getPuzzleByDate(
            @Parameter(description = "Date of the puzzle to retrieve (Format: YYYY-MM-DD)", required = true, example = "2026-10-01") @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminService.getPuzzleDetailsByDate(date));
    }

    @Operation(summary = "Get an overview of all system puzzles", description = "Returns a complete overview list of all puzzles in the system (past, present, and future).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of puzzles retrieved successfully", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PuzzleAdminResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized user", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required", content = @Content)
    })
    @GetMapping("/all")
    public ResponseEntity<List<PuzzleAdminResponse>> getAllPuzzles() {
        return ResponseEntity.ok(adminService.getAllPuzzlesOverview());
    }

    @Operation(summary = "Update valid words associated with a puzzle", description = "Adds or removes words from the set of valid accepted solutions for a given puzzle.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Puzzle words updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PuzzleAdminResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed request body or invalid words", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized user", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Puzzle not found for the given ID", content = @Content)
    })
    @PatchMapping("/{puzzleId}/words")
    public ResponseEntity<PuzzleAdminResponse> updatePuzzleWords(
            @Parameter(description = "Unique ID of the puzzle", required = true, example = "puzzle-123") @PathVariable String puzzleId,
            @Valid @RequestBody UpdatePuzzleWordsRequest request) {
        return ResponseEntity.ok(adminService.updatePuzzleWords(puzzleId, request));
    }

    @Operation(summary = "Update center and outer letters of a puzzle", description = "Modifies the letters that compose the specified puzzle grid and recalculates its associated maximum score.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Puzzle letters updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PuzzleAdminResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid letters or grid rule violations", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized user", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Puzzle not found for the given ID", content = @Content)
    })
    @PutMapping("/{puzzleId}")
    public ResponseEntity<PuzzleAdminResponse> updatePuzzleLetters(
            @Parameter(description = "Unique ID of the puzzle", required = true, example = "puzzle-123") @PathVariable String puzzleId,
            @Valid @RequestBody UpdatePuzzleLettersRequest request) {
        return ResponseEntity.ok(adminService.updatePuzzleLetters(puzzleId, request));
    }
}