package com.beesagono.backend.dto.dictionary;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InvalidWordAttemptStatResponse {
    private String word;
    private Long attemptCount;
}