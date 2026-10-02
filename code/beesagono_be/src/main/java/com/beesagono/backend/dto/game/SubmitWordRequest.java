package com.beesagono.backend.dto.game;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a player's request payload when attempting to submit a word within an active game session.
 */
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
