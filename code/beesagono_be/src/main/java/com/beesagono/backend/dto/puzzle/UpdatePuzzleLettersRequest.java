package com.beesagono.backend.dto.puzzle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

import com.beesagono.backend.validation.DistinctPuzzleLetters;

/**
 * Data Transfer Object representing administrative requests to update the central and outer letter configurations of a daily puzzle.
 */
@Data
@DistinctPuzzleLetters
public class UpdatePuzzleLettersRequest {
    @NotBlank
    @Size(min = 1, max = 1)
    private String centerLetter;

    @NotEmpty
    @Size(min = 6, max = 6)
    private Set<String> outerLetters;
}