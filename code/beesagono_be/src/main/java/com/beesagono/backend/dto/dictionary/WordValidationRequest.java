package com.beesagono.backend.dto.dictionary;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordValidationRequest {

    @Size(min = 4, message = "La parola deve contenere almeno 4 lettere")
    private String word;
}