package com.beesagono.backend.controller;

import com.beesagono.backend.dto.auth.GoogleCheckResponse;
import com.beesagono.backend.dto.auth.GoogleLoginRequest;
import com.beesagono.backend.dto.auth.GoogleRegisterRequest;
import com.beesagono.backend.dto.auth.LoginRequest;
import com.beesagono.backend.dto.auth.LoginResponse;
import com.beesagono.backend.dto.auth.RegisterRequest;
import com.beesagono.backend.dto.auth.RegisterResponse;
import com.beesagono.backend.security.JwtAuthenticationFilter;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private JwtUtils jwtUtils;

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockitoBean
        private TokenBlacklist tokenBlacklist;

        @Nested
        @DisplayName("POST /api/auth/register Tests")
        class RegisterTests {

                @Test
                @DisplayName("Should return 201 Created when registration request is valid")
                void shouldReturnCreatedWhenRequestIsValid() throws Exception {
                        RegisterRequest request = new RegisterRequest();
                        request.setUsername("testuser");
                        request.setEmail("test@example.com");
                        request.setPassword("password123");

                        RegisterResponse response = RegisterResponse.builder()
                                        .id("user-1")
                                        .username("testuser")
                                        .email("test@example.com")
                                        .role("ROLE_USER")
                                        .message("Utente registrato con successo")
                                        .build();

                        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.id").value("user-1"))
                                        .andExpect(jsonPath("$.username").value("testuser"))
                                        .andExpect(jsonPath("$.email").value("test@example.com"))
                                        .andExpect(jsonPath("$.message").value("Utente registrato con successo"));

                        verify(authService, times(1)).register(any(RegisterRequest.class));
                }

                @Test
                @DisplayName("Should return 400 Bad Request when email or username is already taken")
                void shouldReturnBadRequestWhenUserAlreadyExists() throws Exception {
                        RegisterRequest request = new RegisterRequest();
                        request.setUsername("existinguser");
                        request.setEmail("existing@example.com");
                        request.setPassword("password123");

                        when(authService.register(any(RegisterRequest.class)))
                                        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                        "Email già in uso"));

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());

                        verify(authService, times(1)).register(any(RegisterRequest.class));
                }
        }

        @Nested
        @DisplayName("POST /api/auth/login Tests")
        class LoginTests {

                @Test
                @DisplayName("Should return 200 OK with access token when credentials are valid")
                void shouldReturnOkWithTokenWhenCredentialsAreValid() throws Exception {
                        LoginRequest request = new LoginRequest();
                        request.setUsernameOrEmail("testuser");
                        request.setPassword("password123");

                        LoginResponse response = LoginResponse.builder()
                                        .accessToken("jwt.token.value")
                                        .tokenType("Bearer")
                                        .id("user-1")
                                        .username("testuser")
                                        .email("test@example.com")
                                        .role("ROLE_USER")
                                        .build();

                        when(authService.login(any(LoginRequest.class))).thenReturn(response);

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.accessToken").value("jwt.token.value"))
                                        .andExpect(jsonPath("$.username").value("testuser"));

                        verify(authService, times(1)).login(any(LoginRequest.class));
                }

                @Test
                @DisplayName("Should return 401 Unauthorized when credentials are invalid")
                void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
                        LoginRequest request = new LoginRequest();
                        request.setUsernameOrEmail("testuser");
                        request.setPassword("wrongpassword");

                        when(authService.login(any(LoginRequest.class)))
                                        .thenThrow(new BadCredentialsException("Credenziali non valide"));

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isUnauthorized());

                        verify(authService, times(1)).login(any(LoginRequest.class));
                }
        }

        @Nested
        @DisplayName("POST /api/auth/google/check Tests")
        class CheckGoogleUserTests {

                @Test
                @DisplayName("Should return 200 OK with login details when Google user is registered")
                void shouldReturnLoginDetailsWhenGoogleUserExists() throws Exception {
                        GoogleLoginRequest request = new GoogleLoginRequest();
                        request.setIdToken("mock.google.id.token");

                        LoginResponse loginResponse = LoginResponse.builder()
                                        .accessToken("jwt.token.value")
                                        .tokenType("Bearer")
                                        .id("google-user-1")
                                        .username("googleuser")
                                        .email("googleuser@example.com")
                                        .role("ROLE_USER")
                                        .build();

                        GoogleCheckResponse response = GoogleCheckResponse.builder()
                                        .registered(true)
                                        .loginResponse(loginResponse)
                                        .build();

                        when(authService.checkGoogleUser(any(GoogleLoginRequest.class))).thenReturn(response);

                        mockMvc.perform(post("/api/auth/google/check")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.registered").value(true))
                                        .andExpect(jsonPath("$.loginResponse.accessToken").value("jwt.token.value"))
                                        .andExpect(jsonPath("$.loginResponse.id").value("google-user-1"))
                                        .andExpect(jsonPath("$.loginResponse.email").value("googleuser@example.com"));

                        verify(authService, times(1)).checkGoogleUser(any(GoogleLoginRequest.class));
                }

                @Test
                @DisplayName("Should return 200 OK with username suggestion when Google user is new")
                void shouldReturnSuggestedUsernameWhenGoogleUserIsNew() throws Exception {
                        GoogleLoginRequest request = new GoogleLoginRequest();
                        request.setIdToken("mock.google.id.token");

                        GoogleCheckResponse response = GoogleCheckResponse.builder()
                                        .registered(false)
                                        .email("newuser@example.com")
                                        .suggestedUsername("newuser")
                                        .firstName("New")
                                        .lastName("User")
                                        .build();

                        when(authService.checkGoogleUser(any(GoogleLoginRequest.class))).thenReturn(response);

                        mockMvc.perform(post("/api/auth/google/check")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.registered").value(false))
                                        .andExpect(jsonPath("$.email").value("newuser@example.com"))
                                        .andExpect(jsonPath("$.suggestedUsername").value("newuser"))
                                        .andExpect(jsonPath("$.firstName").value("New"))
                                        .andExpect(jsonPath("$.lastName").value("User"));

                        verify(authService, times(1)).checkGoogleUser(any(GoogleLoginRequest.class));
                }

                @Test
                @DisplayName("Should return 400 Bad Request when Google ID token is invalid")
                void shouldReturnBadRequestWhenGoogleTokenIsInvalid() throws Exception {
                        GoogleLoginRequest request = new GoogleLoginRequest();
                        request.setIdToken("invalid.token");

                        when(authService.checkGoogleUser(any(GoogleLoginRequest.class)))
                                        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                        "Token Google non valido"));

                        mockMvc.perform(post("/api/auth/google/check")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());

                        verify(authService, times(1)).checkGoogleUser(any(GoogleLoginRequest.class));
                }
        }

        @Nested
        @DisplayName("POST /api/auth/google/register Tests")
        class RegisterGoogleUserTests {

                @Test
                @DisplayName("Should return 201 Created when Google user registration is successful")
                void shouldReturnCreatedWhenGoogleRegistrationIsSuccessful() throws Exception {
                        GoogleRegisterRequest request = new GoogleRegisterRequest();
                        request.setIdToken("mock.google.id.token");
                        request.setUsername("chosen_username");

                        LoginResponse response = LoginResponse.builder()
                                        .accessToken("jwt.token.value")
                                        .tokenType("Bearer")
                                        .id("google-user-2")
                                        .username("chosen_username")
                                        .email("newuser@example.com")
                                        .role("ROLE_USER")
                                        .build();

                        when(authService.registerGoogleUser(any(GoogleRegisterRequest.class))).thenReturn(response);

                        mockMvc.perform(post("/api/auth/google/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.accessToken").value("jwt.token.value"))
                                        .andExpect(jsonPath("$.id").value("google-user-2"))
                                        .andExpect(jsonPath("$.username").value("chosen_username"))
                                        .andExpect(jsonPath("$.email").value("newuser@example.com"));

                        verify(authService, times(1)).registerGoogleUser(any(GoogleRegisterRequest.class));
                }

                @Test
                @DisplayName("Should return 400 Bad Request when chosen username is already taken during Google registration")
                void shouldReturnBadRequestWhenGoogleUsernameIsTaken() throws Exception {
                        GoogleRegisterRequest request = new GoogleRegisterRequest();
                        request.setIdToken("mock.google.id.token");
                        request.setUsername("taken_username");

                        when(authService.registerGoogleUser(any(GoogleRegisterRequest.class)))
                                        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                        "Username già in uso"));

                        mockMvc.perform(post("/api/auth/google/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());

                        verify(authService, times(1)).registerGoogleUser(any(GoogleRegisterRequest.class));
                }
        }

        @Nested
        @DisplayName("POST /api/auth/logout Tests")
        class LogoutTests {

                @Test
                @DisplayName("Should return 200 OK when logout is successful")
                void shouldReturnOkWhenLogoutIsSuccessful() throws Exception {
                        String token = "Bearer sample.jwt.token";

                        doNothing().when(authService).logout(eq(token));

                        mockMvc.perform(post("/api/auth/logout")
                                        .header("Authorization", token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Logout effettuato con successo"));

                        verify(authService, times(1)).logout(eq(token));
                }
        }
}