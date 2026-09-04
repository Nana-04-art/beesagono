package com.beesagono.backend.controller;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private JwtUtils jwtUtils;

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockitoBean
        private TokenBlacklist tokenBlacklist;

        @TestConfiguration
        static class TestConfig {
                @Bean
                public ObjectMapper objectMapper() {
                        return new ObjectMapper();
                }
        }

        @Test
        @DisplayName("POST /api/auth/register - Success")
        void register_Success() throws Exception {
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
                                .andExpect(jsonPath("$.email").value("test@example.com"));

                verify(authService, times(1)).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("POST /api/auth/login - Success")
        void login_Success() throws Exception {
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
        @DisplayName("POST /api/auth/logout - Success")
        void logout_Success() throws Exception {
                String token = "Bearer sample.jwt.token";

                doNothing().when(authService).logout(eq(token));

                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Logout effettuato con successo"));

                verify(authService, times(1)).logout(eq(token));
        }
}