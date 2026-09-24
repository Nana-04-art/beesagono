package com.beesagono.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object representing the request payload to obtain a new access token using a valid refresh token.
 */
@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token obbligatorio")
    private String refreshToken;
}