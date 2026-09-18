package com.beesagono.backend.dto.dictionary;

import lombok.Data;

/**
 * Data Transfer Object representing search and filter criteria for querying dictionary words.
 */
@Data
public class DictionaryFilterRequest {

    private String search; // Partial word search (case-insensitive)
    private Integer wordLength; // Filter for exact length
    private Boolean isCandidatePangram;// Filter for pangrams
}