package com.beesagono.backend.dto.dictionary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object representing the request payload for adding a single new word to the dictionary,
 * with validation constraints and an optional pangram flag.
 */
@Data
public class AddWordRequest {

    @NotBlank(message = "La parola non può essere vuota")
    @Size(max = 20, message = "La parola può contenere massimo 20 caratteri")
    private String word;

    private Boolean isCandidatePangram;
}
