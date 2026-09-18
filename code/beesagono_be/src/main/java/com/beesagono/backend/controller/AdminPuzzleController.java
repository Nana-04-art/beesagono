package com.beesagono.backend.controller;

import com.beesagono.backend.dto.puzzle.PuzzleAdminResponse;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleLettersRequest;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleWordsRequest;
import com.beesagono.backend.service.AdminService;
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
public class AdminPuzzleController {

    private final AdminService adminService;

    @PostMapping("/generate")
    public ResponseEntity<PuzzleAdminResponse> generatePuzzle(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminService.generateOrResetFuturePuzzle(date));
    }

    @GetMapping
    public ResponseEntity<PuzzleAdminResponse> getPuzzleByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminService.getPuzzleDetailsByDate(date));
    } 

    @GetMapping("/all")
    public ResponseEntity<List<PuzzleAdminResponse>> getAllPuzzles() {
        return ResponseEntity.ok(adminService.getAllPuzzlesOverview());
    }

    @PatchMapping("/{puzzleId}/words")
    public ResponseEntity<PuzzleAdminResponse> updatePuzzleWords(
            @PathVariable String puzzleId,
            @RequestBody @Valid UpdatePuzzleWordsRequest request) {
        return ResponseEntity.ok(adminService.updatePuzzleWords(puzzleId, request));
    }

    @PutMapping("/{puzzleId}")
    public ResponseEntity<PuzzleAdminResponse> updatePuzzleLetters(
            @PathVariable String puzzleId,
            @RequestBody @Valid UpdatePuzzleLettersRequest request) {
        return ResponseEntity.ok(adminService.updatePuzzleLetters(puzzleId, request));
    }
}