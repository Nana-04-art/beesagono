package com.beesagono.backend.dto.dictionary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordValidationResponse {

    private String word;
    private boolean valid;
    private int pointsEarned;
    private boolean isMielegramma;
    private String errorCode; // e.g. "NOT_IN_DICTIONARY", "MISSING_CENTER", "TOO_SHORT"
    private String errorMessage;
}