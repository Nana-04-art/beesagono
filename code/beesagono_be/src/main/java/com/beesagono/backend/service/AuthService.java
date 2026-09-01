package com.beesagono.backend.service;

import com.beesagono.backend.dto.auth.AuthResponse;
import com.beesagono.backend.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    void logout(String token);

}
