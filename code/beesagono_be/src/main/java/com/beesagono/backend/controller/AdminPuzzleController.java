package com.beesagono.backend.controller;

import com.beesagono.backend.dto.puzzle.PuzzleAdminResponse;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleLettersRequest;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleWordsRequest;
import com.beesagono.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/puzzle")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Puzzle Controller", description = "Administrative endpoints for generating and modifying puzzles")
public class AdminPuzzleController {

    private final AdminService adminService;

    @Operation(summary = "Generate or reset puzzle for a future date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Puzzle generated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    @PostMapping("/generate")
    public ResponseEntity<PuzzleAdminResponse> generatePuzzle(
            @Parameter(description = "Date to generate the puzzle for (YYYY-MM-DD)") @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminService.generateOrResetFuturePuzzle(date));
    }

    @Operation(summary = "Get administrative puzzle details by date")
    @GetMapping
    public ResponseEntity<PuzzleAdminResponse> getPuzzleByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminService.getPuzzleDetailsByDate(date));
    }

    @Operation(summary = "Get an overview of all system puzzles")
    @GetMapping("/all")
    public ResponseEntity<List<PuzzleAdminResponse>> getAllPuzzles() {
        return ResponseEntity.ok(adminService.getAllPuzzlesOverview());
    }

    @Operation(summary = "Update valid words associated with a puzzle")
    @PatchMapping("/{puzzleId}/words")
    public ResponseEntity<PuzzleAdminResponse> updatePuzzleWords(
            @PathVariable String puzzleId,
            @RequestBody @Valid UpdatePuzzleWordsRequest request) {
        return ResponseEntity.ok(adminService.updatePuzzleWords(puzzleId, request));
    }

    @Operation(summary = "Update puzzle letters")
    @PutMapping("/{puzzleId}")
    public ResponseEntity<PuzzleAdminResponse> updatePuzzleLetters(
            @PathVariable String puzzleId,
            @RequestBody @Valid UpdatePuzzleLettersRequest request) {
        return ResponseEntity.ok(adminService.updatePuzzleLetters(puzzleId, request));
    }
}