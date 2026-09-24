package com.beesagono.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCheckResponse {
    private boolean registered;
    private LoginResponse loginResponse;
    private String email;
    private String suggestedUsername;
    private String firstName;
    private String lastName;
}