package com.beesagono.backend.controller;

import com.beesagono.backend.dto.stats.PlayerStatsResponse;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.security.GlobalExceptionHandler;
import com.beesagono.backend.security.JwtAuthenticationFilter;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.PlayerStatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlayerStatsController.class)
@Import({ GlobalExceptionHandler.class, PlayerStatsControllerTest.TestConfig.class })
@AutoConfigureMockMvc(addFilters = false)
class PlayerStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerStatsService playerStatsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private TokenBlacklist tokenBlacklist;

    private User testUser;
    private UserDetailsImpl principal;

    @BeforeEach
    void setUp() {
        testUser = createTestUser("user-1", "testuser", "user@example.com");
        principal = createTestPrincipal(testUser);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // --- GET /api/stats/me ---

    @Nested
    @DisplayName("GET /api/stats/me Tests")
    @ContextConfiguration(classes = TestConfig.class)
    class GetMyStatsTests {

        @Test
        @DisplayName("GET /api/stats/me - Success")
        void getMyStats_Success() throws Exception {
            PlayerStatsResponse response = createPlayerStatsResponse("user-1");

            when(playerStatsService.getPlayerStats("user-1")).thenReturn(response);

            mockMvc.perform(get("/api/stats/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("user-1"))
                    .andExpect(jsonPath("$.gamesPlayed").value(10))
                    .andExpect(jsonPath("$.gamesCompleted").value(8))
                    .andExpect(jsonPath("$.currentStreak").value(3))
                    .andExpect(jsonPath("$.maxStreak").value(5))
                    .andExpect(jsonPath("$.totalScoreEarned").value(500))
                    .andExpect(jsonPath("$.longestWordFound").value("ALBERO"))
                    .andExpect(jsonPath("$.averageScorePerGame").value(50.0))
                    .andExpect(jsonPath("$.completionRate").value(80.0));

            verify(playerStatsService, times(1)).getPlayerStats("user-1");
        }

        @Test
        @DisplayName("GET /api/stats/me - Stats Not Found Throws 404")
        void getMyStats_NotFound() throws Exception {
            when(playerStatsService.getPlayerStats("user-1"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Statistiche utente non trovate"));

            mockMvc.perform(get("/api/stats/me"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Statistiche utente non trovate"));

            verify(playerStatsService, times(1)).getPlayerStats("user-1");
        }

        @Test
        @DisplayName("GET /api/stats/me - Generic Internal Server Error Throws 500")
        void getMyStats_InternalServerError() throws Exception {
            when(playerStatsService.getPlayerStats("user-1"))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/stats/me"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.message").value("Si è verificato un errore interno al server."));

            verify(playerStatsService, times(1)).getPlayerStats("user-1");
        }
    }

    // --- Helper Methods ---

    private User createTestUser(String id, String username, String email) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .build();
    }

    private UserDetailsImpl createTestPrincipal(User user) {
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private PlayerStatsResponse createPlayerStatsResponse(String userId) {
        return PlayerStatsResponse.builder()
                .userId(userId)
                .gamesPlayed(10)
                .gamesCompleted(8)
                .currentStreak(3)
                .maxStreak(5)
                .totalScoreEarned(500)
                .longestWordFound("ALBERO")
                .averageScorePerGame(50.0)
                .completionRate(80.0)
                .build();
    }

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
}