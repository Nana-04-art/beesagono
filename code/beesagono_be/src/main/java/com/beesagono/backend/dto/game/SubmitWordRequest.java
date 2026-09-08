package com.beesagono.backend.dto.game;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitWordRequest {

    @NotBlank(message = "Session ID obbligatorio")
    private String sessionId;

    @NotBlank(message = "La parola tentata non può essere vuota")
    private String word;
}
