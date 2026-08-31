package com.beesagono.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryWordResponse {

    private String word;
    private Integer wordLength;
    private Integer uniqueLettersCount;
    private Boolean isCandidatePangram;
    private String addedByUsername;
    private LocalDateTime addedAt;
}
