package com.beesagono.backend.dto.puzzle;

import lombok.Data;

import java.util.Set;

@Data
public class UpdatePuzzleWordsRequest {
    private Set<String> wordsToAdd;
    private Set<String> wordsToRemove;
}