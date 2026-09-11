package com.beesagono.backend.dto.puzzle;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class PuzzleAdminResponse {
    private String id;
    private LocalDate puzzleDate;
    private String centerLetter;
    private Set<String> outerLetters;
    private int maxScore;
    private boolean editable;
    private List<String> validWords;
    private Long activeSessionsCount;
    private Long completedSessionsCount;
}