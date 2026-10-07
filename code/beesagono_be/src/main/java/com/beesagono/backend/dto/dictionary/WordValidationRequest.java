package com.beesagono.backend.dto.dictionary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "La data del puzzle è obbligatoria")
    private String puzzleDate;

    @NotBlank(message = "La parola non può essere vuota")
    @Size(min = 4, message = "La parola deve contenere almeno 4 lettere")
    private String word;
}