package com.beesagono.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddWordRequest {

    @NotBlank(message = "La parola non può essere vuota")
    @Size(max = 100, message = "La parola può contenere massimo 100 caratteri")
    private String word;

    private Boolean isCandidatePangram;
}
