package com.beesagono.backend.controller;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.security.GlobalExceptionHandler;
import com.beesagono.backend.security.JwtAuthenticationFilter;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.PuzzleService;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PuzzleController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class PuzzleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PuzzleService puzzleService;

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
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // --- GET /api/puzzles/today ---

    @Nested 
    @DisplayName("GET /api/puzzles/today Tests")
    class GetTodayPuzzleTests {

        @Test
        @DisplayName("Should return 200 OK with puzzle data when puzzle exists or is generated")
        void shouldReturnOkWithPuzzleDataWhenPuzzleExists() throws Exception {
            DailyPuzzleResponse response = createDailyPuzzleResponse("puz-1", LocalDate.now(), "A", 100);

        when(puzzleService.getTodayPuzzle()).thenReturn(response);

            mockMvc.perform(get("/api/puzzles/today")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("puz-1"))
                    .andExpect(jsonPath("$.centerLetter").value("A"))
                    .andExpect(jsonPath("$.maxScore").value(100));

        verify(puzzleService, times(1)).getTodayPuzzle();
    }

        @Test
        @DisplayName("Should return 404 Not Found when puzzle is missing")
        void shouldReturnNotFoundWhenPuzzleDoesNotExist() throws Exception {
            when(puzzleService.getTodayPuzzle())
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Puzzle del giorno non trovato"));

            mockMvc.perform(get("/api/puzzles/today")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Puzzle del giorno non trovato"));

            verify(puzzleService, times(1)).getTodayPuzzle();
        }

        @Test
        @DisplayName("Should return 500 Internal Server Error when generic exception occurs")
        void shouldReturnInternalServerErrorWhenGenericErrorOccurs() throws Exception {
            when(puzzleService.getTodayPuzzle())
                    .thenThrow(new RuntimeException("Database connection failure"));

            mockMvc.perform(get("/api/puzzles/today"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.message").value("Si è verificato un errore interno al server."));

            verify(puzzleService, times(1)).getTodayPuzzle();
        }
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

    private DailyPuzzleResponse createDailyPuzzleResponse(String id, LocalDate date, String centerLetter,
            int maxScore) {
        return DailyPuzzleResponse.builder()
                .id(id)
                .puzzleDate(date)
                .centerLetter(centerLetter)
                .maxScore(maxScore)
                .outerLetters(Set.of("B", "C", "D", "E", "F", "G"))
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