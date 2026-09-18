package com.beesagono.backend.controller;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.dto.puzzle.WordSubmissionRequest;
import com.beesagono.backend.dto.puzzle.WordSubmissionResponse;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.PuzzleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController 
@RequestMapping ("/api/puzzles")
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
            @RequestBody WordSubmissionRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        return ResponseEntity.ok(
            puzzleService.validateAndScoreWord(userDetails.getId(), puzzleId, request.word())
        );
    }
}