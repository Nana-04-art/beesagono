package com.beesagono.backend.dto.puzzle;

import lombok.Data;

import java.util.Set;

/**
 * Data Transfer Object representing administrative requests to dynamically add or remove valid solution words from a daily puzzle.
 */
@Data
public class UpdatePuzzleWordsRequest {
    private Set<String> wordsToAdd;
    private Set<String> wordsToRemove;
}