package com.beesagono.backend.dto.dictionary;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data Transfer Object representing aggregated statistics for an invalid word attempt,
 * including the attempted word string and its total rejection count.
 */
@Data
@AllArgsConstructor
public class InvalidWordAttemptStatResponse {
    private String word;
    private Long attemptCount;
}