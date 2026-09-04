package com.beesagono.backend.dto.dictionary;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchAddWordRequest {

    @NotEmpty(message = "La lista di parole non può essere vuota")
    private List<String> words;
}