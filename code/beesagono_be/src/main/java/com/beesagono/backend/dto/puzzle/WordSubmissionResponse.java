package com.beesagono.backend.dto.puzzle;

import com.beesagono.backend.enums.ErrorTypeCode;

public record WordSubmissionResponse(
        boolean valid,
        String word,
        int score,
        boolean isMielegramma,
        ErrorTypeCode errorCode,
        String errorMessage) {
}