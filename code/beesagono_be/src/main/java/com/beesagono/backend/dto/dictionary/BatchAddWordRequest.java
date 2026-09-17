package com.beesagono.backend.dto.dictionary;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object representing the request payload for bulk adding multiple words to the dictionary.
 */
@Data
public class BatchAddWordRequest {

    @NotEmpty(message = "La lista di parole non può essere vuota")
    private List<String> words;
}