package com.beesagono.backend.controller;

import com.beesagono.backend.dto.dictionary.WordValidationRequest;
import com.beesagono.backend.dto.dictionary.WordValidationResponse;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.security.GlobalExceptionHandler;
import com.beesagono.backend.security.JwtAuthenticationFilter;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.service.AdminService;
import com.beesagono.backend.service.DictionaryService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DictionaryController.class)
@Import({ GlobalExceptionHandler.class, DictionaryControllerTest.TestConfig.class })
@AutoConfigureMockMvc(addFilters = false)
class DictionaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DictionaryService dictionaryService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private TokenBlacklist tokenBlacklist;

    @Nested
    @DisplayName("Guest Validation Endpoint Tests")
    class GuestValidationTests {

        @Test
        @DisplayName("POST /api/dictionary/validate - Success")
        void validateWordForGuest_Success() throws Exception {
            WordValidationRequest request = new WordValidationRequest();
            request.setWord("CASA");

            WordValidationResponse response = WordValidationResponse.builder()
                    .word("CASA")
                    .valid(true)
                    .pointsEarned(1)
                    .isMielegramma(false)
                    .build();

            when(dictionaryService.validateWordForGuest("CASA")).thenReturn(response);

            mockMvc.perform(post("/api/dictionary/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.word").value("CASA"))
                    .andExpect(jsonPath("$.pointsEarned").value(1));

            verify(dictionaryService, times(1)).validateWordForGuest("CASA");
        }

        @Test
        @DisplayName("POST /api/dictionary/validate - Invalid Word Returns Validation Payload")
        void validateWordForGuest_InvalidWord() throws Exception {
            WordValidationRequest request = new WordValidationRequest();
            request.setWord("APE");

            WordValidationResponse response = WordValidationResponse.builder()
                    .word("APE")
                    .valid(false)
                    .errorCode("TOO_SHORT")
                    .errorMessage("La parola deve contenere almeno 4 lettere.")
                    .build();

            when(dictionaryService.validateWordForGuest("APE")).thenReturn(response);

            mockMvc.perform(post("/api/dictionary/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.errorCode").value("TOO_SHORT"));

            verify(dictionaryService, times(1)).validateWordForGuest("APE");
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}