package com.beesagono.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class CreatePuzzleRequest {

    @NotNull(message = "Data obbligatoria")
    private LocalDate puzzleDate;

    @NotBlank(message = "Lettera centrale obbligatoria")
    @Size(min = 1, max = 1, message = "Deve essere un singolo carattere")
    private String centerLetter;

    @NotEmpty(message = "Devi inserire le lettere esterne")
    private Set<String> outerLetters;

    @NotNull(message = "Punteggio massimo obbligatorio")
    private Integer maxScore;

    private String seed;
}
