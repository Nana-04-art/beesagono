package com.beesagono.backend.controller;

import com.beesagono.backend.dto.game.GameBulkSyncRequest;
import com.beesagono.backend.dto.game.GameSessionResponse;
import com.beesagono.backend.dto.game.GameSyncRequest;
import com.beesagono.backend.dto.game.SubmitWordRequest;
import com.beesagono.backend.dto.game.SubmitWordResponse;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.security.JwtAuthenticationFilter;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameController.class)
@AutoConfigureMockMvc(addFilters = false)
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private TokenBlacklist tokenBlacklist;

    private UserDetailsImpl principal;

    @TestConfiguration
    static class TestConfig implements WebMvcConfigurer {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                }

                @Override
                public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                    if (SecurityContextHolder.getContext().getAuthentication() != null) {
                        return SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    }
                    return null;
                }
            });
        }
    }

    @BeforeEach
    void setUp() {
        principal = new UserDetailsImpl(
                "user-1",
                "testuser",
                "user@example.com",
                "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // --- getTodaySession ---

    @Test
    @DisplayName("GET /api/game/session/today - Success")
    void getTodaySession_Success() throws Exception {
        GameSessionResponse response = new GameSessionResponse();

        when(gameService.getOrCreateTodaySession(any())).thenReturn(response);

        mockMvc.perform(get("/api/game/session/today"))
                .andExpect(status().isOk());

        verify(gameService, times(1)).getOrCreateTodaySession(any());
    }

    // --- submitWord ---

    @Test
    @DisplayName("POST /api/game/submit - Success")
    void submitWord_Success() throws Exception {
        SubmitWordRequest request = new SubmitWordRequest();
        request.setSessionId("session-123");
        request.setWord("CASA");

        SubmitWordResponse response = new SubmitWordResponse();

        when(gameService.validateAndScoreWord(any(SubmitWordRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/game/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gameService, times(1)).validateAndScoreWord(any(SubmitWordRequest.class), any());
    }

    // --- syncLocalProgress ---

    @Test
    @DisplayName("POST /api/game/sync - Success")
    void syncLocalProgress_Success() throws Exception {
        GameBulkSyncRequest request = new GameBulkSyncRequest();
        request.setGames(List.of(new GameSyncRequest()));

        GameSessionResponse sessionResponse = new GameSessionResponse();
        List<GameSessionResponse> responseList = List.of(sessionResponse);

        when(gameService.syncLocalProgress(any(GameBulkSyncRequest.class), any()))
                .thenReturn(responseList);

        mockMvc.perform(post("/api/game/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(gameService, times(1)).syncLocalProgress(any(GameBulkSyncRequest.class), any());
    }
}