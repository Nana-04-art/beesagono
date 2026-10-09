package com.beesagono.backend.dto.dictionary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Data Transfer Object representing detailed information about a single dictionary word entry.
 */
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
    private Date addedAt;
}