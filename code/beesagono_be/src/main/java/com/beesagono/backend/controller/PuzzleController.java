package com.beesagono.backend.controller;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.service.PuzzleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/puzzles")
@RequiredArgsConstructor
public class PuzzleController {

    private final PuzzleService puzzleService;

    @GetMapping("/today")
    public ResponseEntity<DailyPuzzleResponse> getTodayPuzzle() {
        return ResponseEntity.ok(puzzleService.getTodayPuzzle());
    }
}