package com.beesagono.backend.controller;

import com.beesagono.backend.dto.stats.LeaderboardEntryDto;
import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.dto.stats.RankDistributionResponse;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.security.GlobalExceptionHandler;
import com.beesagono.backend.security.JwtAuthenticationFilter;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.PlayerSeasonService;
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
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlayerSeasonController.class)
@Import({GlobalExceptionHandler.class, PlayerSeasonControllerTest.TestConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class PlayerSeasonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerSeasonService playerSeasonService;

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

    // --- GET /api/seasons/me ---

    @Nested
    @DisplayName("GET /api/seasons/me Tests")
    @ContextConfiguration(classes = TestConfig.class)
    class GetMySeasonStatsTests {

        @Test
        @DisplayName("GET /api/seasons/me - Success")
        void getMySeasonStats_Success() throws Exception {
            PlayerSeasonResponse response = createPlayerSeasonResponse(2026);

            when(playerSeasonService.getCurrentSeasonStats("user-1")).thenReturn(response);

            mockMvc.perform(get("/api/seasons/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.year").value(2026))
                    .andExpect(jsonPath("$.highestTierAchieved").value("Uovo d'Ape"))
                    .andExpect(jsonPath("$.basePoints").value(1000))
                    .andExpect(jsonPath("$.bonusPoints").value(200))
                    .andExpect(jsonPath("$.totalPoints").value(1200));

            verify(playerSeasonService, times(1)).getCurrentSeasonStats("user-1");
        }

        @Test
        @DisplayName("GET /api/seasons/me - Stats Not Found Throws 404")
        void getMySeasonStats_NotFound() throws Exception {
            when(playerSeasonService.getCurrentSeasonStats("user-1"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Statistiche stagionali non trovate"));

            mockMvc.perform(get("/api/seasons/me"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Statistiche stagionali non trovate"));

            verify(playerSeasonService, times(1)).getCurrentSeasonStats("user-1");
        }

        @Test
        @DisplayName("GET /api/seasons/me - Generic Internal Server Error Throws 500")
        void getMySeasonStats_InternalServerError() throws Exception {
            when(playerSeasonService.getCurrentSeasonStats("user-1"))
                    .thenThrow(new RuntimeException("Database connection failure"));

            mockMvc.perform(get("/api/seasons/me"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.message").value("Si è verificato un errore interno al server."));

            verify(playerSeasonService, times(1)).getCurrentSeasonStats("user-1");
        }
    }

    // --- GET /api/seasons/history ---

    @Nested
    @DisplayName("GET /api/seasons/history Tests")
    @ContextConfiguration(classes = TestConfig.class)
    class GetMySeasonHistoryTests {

        @Test
        @DisplayName("GET /api/seasons/history - Success")
        void getMySeasonHistory_Success() throws Exception {
            List<PlayerSeasonResponse> history = List.of(
                    createPlayerSeasonResponse(2026),
                    createPlayerSeasonResponse(2025));

            when(playerSeasonService.getPlayerSeasonHistory("user-1")).thenReturn(history);

            mockMvc.perform(get("/api/seasons/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].year").value(2026))
                    .andExpect(jsonPath("$[0].totalPoints").value(1200))
                    .andExpect(jsonPath("$[1].year").value(2025))
                    .andExpect(jsonPath("$[1].totalPoints").value(1200));

            verify(playerSeasonService, times(1)).getPlayerSeasonHistory("user-1");
        }
    }

    // --- GET /api/seasons/rank-distribution ---

    @Nested
    @DisplayName("GET /api/seasons/rank-distribution Tests")
    @ContextConfiguration(classes = TestConfig.class)
    class GetRankDistributionTests {

        @Test
        @DisplayName("GET /api/seasons/rank-distribution - Success")
        void getRankDistribution_Success() throws Exception {
            RankDistributionResponse distribution = RankDistributionResponse.builder()
                    .seasonYear(2026)
                    .totalPlayers(100L)
                    .tierCounts(Map.of("Uovo d'Ape", 50L, "Ape Operosa", 50L))
                    .build();

            when(playerSeasonService.getRankDistribution()).thenReturn(distribution);

            mockMvc.perform(get("/api/seasons/rank-distribution"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.seasonYear").value(2026))
                    .andExpect(jsonPath("$.totalPlayers").value(100))
                    .andExpect(jsonPath("$.tierCounts['Uovo d\\'Ape']").value(50))
                    .andExpect(jsonPath("$.tierCounts['Ape Operosa']").value(50));

            verify(playerSeasonService, times(1)).getRankDistribution();
        }
    }

    // --- GET /api/seasons/leaderboard ---

    @Nested
    @DisplayName("GET /api/seasons/leaderboard Tests")
    @ContextConfiguration(classes = TestConfig.class)
    class GetLeaderboardTests {

        @Test
        @DisplayName("GET /api/seasons/leaderboard - Success with default limit")
        void getLeaderboard_DefaultLimit_Success() throws Exception {
            LeaderboardEntryDto entry = LeaderboardEntryDto.builder()
                    .username("testuser")
                    .totalPoints(1200)
                    .highestTierAchieved("Uovo d'Ape")
                    .rankPosition(1)
                    .build();

            when(playerSeasonService.getTopLeaderboard(10)).thenReturn(List.of(entry));

            mockMvc.perform(get("/api/seasons/leaderboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].username").value("testuser"))
                    .andExpect(jsonPath("$[0].totalPoints").value(1200))
                    .andExpect(jsonPath("$[0].highestTierAchieved").value("Uovo d'Ape"))
                    .andExpect(jsonPath("$[0].rankPosition").value(1));

            verify(playerSeasonService, times(1)).getTopLeaderboard(10);
        }

        @Test
        @DisplayName("GET /api/seasons/leaderboard - Success with custom limit")
        void getLeaderboard_CustomLimit_Success() throws Exception {
            when(playerSeasonService.getTopLeaderboard(5)).thenReturn(List.of());

            mockMvc.perform(get("/api/seasons/leaderboard").param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(playerSeasonService, times(1)).getTopLeaderboard(5);
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

    private PlayerSeasonResponse createPlayerSeasonResponse(int year) {
        return PlayerSeasonResponse.builder()
                .year(year)
                .highestTierAchieved("Uovo d'Ape")
                .basePoints(1000)
                .bonusPoints(200)
                .totalPoints(1200)
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