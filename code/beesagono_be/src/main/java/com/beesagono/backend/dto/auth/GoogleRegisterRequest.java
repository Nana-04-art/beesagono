package com.beesagono.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleRegisterRequest {
    @NotBlank(message = "IdToken è obbligatorio")
    private String idToken;

    @NotBlank(message = "Username è obbligatorio")
    private String username;
}