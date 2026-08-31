package com.beesagono.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitWordResponse {

    private Boolean success;
    private String word;
    private Integer pointsEarned;
    private Integer currentScore;
    private String currentRankLabel;
    private Boolean isMielegramma;
    private String errorCode; // Present only if success = false (e.g. TOO_SHORT, MISSING_CENTER)
    private String errorMessage;
}
