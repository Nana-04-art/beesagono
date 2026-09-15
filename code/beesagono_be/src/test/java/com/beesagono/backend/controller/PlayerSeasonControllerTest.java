package com.beesagono.backend.controller;

import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
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

@WebMvcTest(PlayerSeasonController.class)
@Import(GlobalExceptionHandler.class)
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

    // --- GET /api/stats/me ---

    @Test
    @DisplayName("GET /api/stats/me - Success")
    void getMySeasonStats_Success() throws Exception {
        PlayerSeasonResponse response = createPlayerSeasonResponse();

        when(playerSeasonService.getCurrentSeasonStats("user-1")).thenReturn(response);

        mockMvc.perform(get("/api/stats/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.highestTierAchieved").value("Uovo d'Ape"))
                .andExpect(jsonPath("$.gamesPlayed").value(10))
                .andExpect(jsonPath("$.gamesCompleted").value(8))
                .andExpect(jsonPath("$.currentStreak").value(3))
                .andExpect(jsonPath("$.maxStreak").value(5))
                .andExpect(jsonPath("$.basePoints").value(1000))
                .andExpect(jsonPath("$.bonusPoints").value(200))
                .andExpect(jsonPath("$.totalPoints").value(1200));

        verify(playerSeasonService, times(1)).getCurrentSeasonStats("user-1");
    }

    @Test
    @DisplayName("GET /api/stats/me - Stats Not Found Throws 404")
    void getMySeasonStats_NotFound() throws Exception {
        when(playerSeasonService.getCurrentSeasonStats("user-1"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Statistiche stagionali non trovate"));

        mockMvc.perform(get("/api/stats/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Statistiche stagionali non trovate"));

        verify(playerSeasonService, times(1)).getCurrentSeasonStats("user-1");
    }

    @Test
    @DisplayName("GET /api/stats/me - Generic Internal Server Error Throws 500")
    void getMySeasonStats_InternalServerError() throws Exception {
        when(playerSeasonService.getCurrentSeasonStats("user-1"))
                .thenThrow(new RuntimeException("Database connection failure"));

        mockMvc.perform(get("/api/stats/me"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Si è verificato un errore interno al server."));

        verify(playerSeasonService, times(1)).getCurrentSeasonStats("user-1");
    }

    // --- Private Helper Methods ---

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

    private PlayerSeasonResponse createPlayerSeasonResponse() {
        return PlayerSeasonResponse.builder()
                .year(2026)
                .highestTierAchieved("Uovo d'Ape")
                .gamesPlayed(10)
                .gamesCompleted(8)
                .currentStreak(3)
                .maxStreak(5)
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