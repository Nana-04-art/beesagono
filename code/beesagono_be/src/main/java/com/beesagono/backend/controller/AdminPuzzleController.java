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
@Tag(name = "Admin Puzzle Controller", description = "Endpoints amministrativi per generazione e modifica puzzle")
public class AdminPuzzleController {

    private final AdminService adminService;

    @Operation(summary = "Genera o resetta il puzzle per una data futura")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Puzzle generato con successo"),
            @ApiResponse(responseCode = "403", description = "Accesso negato - Richiesto ruolo ADMIN")
    })
    @PostMapping("/generate")
    public ResponseEntity<PuzzleAdminResponse> generatePuzzle(
            @Parameter(description = "Data per cui generare il puzzle (YYYY-MM-DD)") @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminService.generateOrResetFuturePuzzle(date));
    }

    @Operation(summary = "Ottieni dettagli amministrativi di un puzzle tramite data")
    @GetMapping
    public ResponseEntity<PuzzleAdminResponse> getPuzzleByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminService.getPuzzleDetailsByDate(date));
    }

    @Operation(summary = "Ottieni la panoramica di tutti i puzzle a sistema")
    @GetMapping("/all")
    public ResponseEntity<List<PuzzleAdminResponse>> getAllPuzzles() {
        return ResponseEntity.ok(adminService.getAllPuzzlesOverview());
    }

    @Operation(summary = "Aggiorna le parole valide associate a un puzzle")
    @PatchMapping("/{puzzleId}/words")
    public ResponseEntity<PuzzleAdminResponse> updatePuzzleWords(
            @PathVariable String puzzleId,
            @RequestBody @Valid UpdatePuzzleWordsRequest request) {
        return ResponseEntity.ok(adminService.updatePuzzleWords(puzzleId, request));
    }

    @Operation(summary = "Aggiorna le lettere di un puzzle")
    @PutMapping("/{puzzleId}")
    public ResponseEntity<PuzzleAdminResponse> updatePuzzleLetters(
            @PathVariable String puzzleId,
            @RequestBody @Valid UpdatePuzzleLettersRequest request) {
        return ResponseEntity.ok(adminService.updatePuzzleLetters(puzzleId, request));
    }
}