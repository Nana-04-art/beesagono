package com.beesagono.backend.controller;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.dto.puzzle.WordSubmissionRequest;
import com.beesagono.backend.dto.puzzle.WordSubmissionResponse;
import com.beesagono.backend.service.PuzzleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/puzzles")
@RequiredArgsConstructor
public class PuzzleController {

    private final PuzzleService puzzleService;

    @GetMapping("/today")
    public ResponseEntity<DailyPuzzleResponse> getTodayPuzzle() {
        return ResponseEntity.ok(puzzleService.getTodayPuzzle());
    }

    @PostMapping("/{puzzleId}/submit")
    public ResponseEntity<WordSubmissionResponse> submitWord(
            @PathVariable String puzzleId,
            @RequestBody WordSubmissionRequest request) {
        return ResponseEntity.ok(puzzleService.validateAndScoreWord(puzzleId, request.word()));
    }
}