package com.beesagono.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object representing the user authentication request credentials.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Username o email obbligatori")
    private String usernameOrEmail;

    @NotBlank(message = "Password obbligatoria")
    private String password;
}