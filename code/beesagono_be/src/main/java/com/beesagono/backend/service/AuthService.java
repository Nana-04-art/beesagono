package com.beesagono.backend.service;

import com.beesagono.backend.dto.auth.GoogleCheckResponse;
import com.beesagono.backend.dto.auth.GoogleLoginRequest;
import com.beesagono.backend.dto.auth.GoogleRegisterRequest;
import com.beesagono.backend.dto.auth.LoginRequest;
import com.beesagono.backend.dto.auth.LoginResponse;
import com.beesagono.backend.dto.auth.RegisterRequest;
import com.beesagono.backend.dto.auth.RegisterResponse;

/**
 * Service interface handling user authentication lifecycle events including registration, login, and session logout.
 */
public interface AuthService {

    /**
     * Registers a new player account in the system.
     *
     * @param request payload containing registration details and credentials
     * @return {@link RegisterResponse} confirming account creation
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * Authenticates user credentials and issues JWT access tokens upon success.
     *
     * @param request payload containing user login credentials
     * @return {@link LoginResponse} containing generated JWT tokens and user metadata
     */
    LoginResponse login(LoginRequest request);

    /**
     * Revokes an active JWT token and invalidates the user session by placing it in the blacklist.
     *
     * @param token raw JWT string to revoke
     */
    void logout(String token);

    GoogleCheckResponse checkGoogleUser(GoogleLoginRequest request);

    LoginResponse registerGoogleUser(GoogleRegisterRequest request);

}
