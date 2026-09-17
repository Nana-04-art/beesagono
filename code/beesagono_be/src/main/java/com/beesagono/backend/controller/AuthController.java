package com.beesagono.backend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beesagono.backend.dto.auth.GoogleCheckResponse;
import com.beesagono.backend.dto.auth.GoogleLoginRequest;
import com.beesagono.backend.dto.auth.GoogleRegisterRequest;
import com.beesagono.backend.dto.auth.LoginRequest;
import com.beesagono.backend.dto.auth.LoginResponse;
import com.beesagono.backend.dto.auth.RegisterRequest;
import com.beesagono.backend.dto.auth.RegisterResponse;
import com.beesagono.backend.service.AuthService;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Controller", description = "Endpoints for user registration, authentication, OAuth2 Google flow, and session management")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user", description = "Creates a new standard user account in the system using credentials.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid registration request payload or validation error", content = @Content),
            @ApiResponse(responseCode = "409", description = "Username or email is already in use", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during user registration", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "User authentication (Login)", description = "Authenticates user credentials and returns JWT authentication tokens.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid username/email or password credentials", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during authentication", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "User logout and token invalidation", description = "Invalidates the active session and revokes the provided JWT bearer token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Missing or malformed Authorization header", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during logout", content = @Content)
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @Parameter(description = "Bearer JWT token to be invalidated", required = true, example = "Bearer eyJhbGciOiJIUzI1NiJ9...") @RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logout effettuato con successo"));
    }

    @Operation(summary = "Check Google OAuth account status", description = "Verifies whether a Google user already exists in the system or requires registration completion.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Google check executed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GoogleCheckResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Google ID token payload", content = @Content),
            @ApiResponse(responseCode = "401", description = "Expired or unverified Google token", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during Google account verification", content = @Content)
    })
    @PostMapping("/google/check")
    public ResponseEntity<GoogleCheckResponse> checkGoogleUser(@Valid @RequestBody GoogleLoginRequest request) {
        GoogleCheckResponse response = authService.checkGoogleUser(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Register user via Google OAuth", description = "Completes user registration using Google OAuth identity and issues JWT tokens.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Google user registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Google registration payload or missing mandatory fields", content = @Content),
            @ApiResponse(responseCode = "409", description = "Username or email linked to Google account already exists", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during Google user registration", content = @Content)
    })
    @PostMapping("/google/register")
    public ResponseEntity<LoginResponse> registerGoogleUser(@Valid @RequestBody GoogleRegisterRequest request) {
        LoginResponse response = authService.registerGoogleUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}