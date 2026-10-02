package com.beesagono.backend.controller;

import com.beesagono.backend.dto.dictionary.WordValidationRequest;
import com.beesagono.backend.dto.dictionary.WordValidationResponse;
import com.beesagono.backend.security.GlobalExceptionHandler;
import com.beesagono.backend.security.JwtAuthenticationFilter;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.service.DictionaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DictionaryController.class)
@Import({ GlobalExceptionHandler.class, ObjectMapper.class })
@AutoConfigureMockMvc(addFilters = false)
class DictionaryControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private DictionaryService dictionaryService;

        @MockitoBean
        private JwtUtils jwtUtils;

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockitoBean
        private TokenBlacklist tokenBlacklist;

        @Test
        @DisplayName("POST /api/dictionary/validate - Should return 200 OK when word validation succeeds")
        void validateWordGuest_Success() throws Exception {
                String puzzleDate = "2026-10-02";
                WordValidationRequest request = new WordValidationRequest(puzzleDate, "CASA");

                WordValidationResponse mockResponse = WordValidationResponse.builder()
                                .valid(true)
                                .word("CASA")
                                .pointsEarned(1)
                                .isMielegramma(false)
                                .build();

                when(dictionaryService.validateWordForGuest(eq(puzzleDate), eq("CASA")))
                                .thenReturn(mockResponse);

                mockMvc.perform(post("/api/dictionary/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.valid").value(true))
                                .andExpect(jsonPath("$.word").value("CASA"))
                                .andExpect(jsonPath("$.pointsEarned").value(1))
                                .andExpect(jsonPath("$.mielegramma").value(false));
        }

        @Test
        @DisplayName("POST /api/dictionary/validate - Should return 400 BAD REQUEST on validation failure (invalid body)")
        void validateWordGuest_InvalidRequestPayload() throws Exception {
                WordValidationRequest invalidRequest = new WordValidationRequest("2026-10-02", "SOL");

                mockMvc.perform(post("/api/dictionary/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/dictionary/validate - Should return 404 NOT FOUND if puzzle for date does not exist")
        void validateWordGuest_PuzzleNotFound() throws Exception {
                String puzzleDate = "2026-10-02";
                WordValidationRequest request = new WordValidationRequest(puzzleDate, "CASA");

                when(dictionaryService.validateWordForGuest(eq(puzzleDate), eq("CASA")))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Puzzle not found for date"));

                mockMvc.perform(post("/api/dictionary/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }
}