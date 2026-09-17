package com.beesagono.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing the response payload returned after a successful user registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private String id;
    private String username;
    private String email;
    private String role;
    private String message;
}