package com.beesagono.backend.dto.game;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder 
@NoArgsConstructor
@AllArgsConstructor
public class SubmitWordRequest {

    @NotBlank(message = "Session ID obbligatorio")
    private String sessionId;

    @NotBlank(message = "La parola tentata non può essere vuota")
    private String word;
}
