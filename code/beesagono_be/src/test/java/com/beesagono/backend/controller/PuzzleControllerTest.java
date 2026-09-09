package com.beesagono.backend.controller;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.dto.puzzle.WordSubmissionRequest;
import com.beesagono.backend.dto.puzzle.WordSubmissionResponse;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.enums.ErrorTypeCode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PuzzleController.class)
@Import({ GlobalExceptionHandler.class, PuzzleControllerTest.TestConfig.class })
@AutoConfigureMockMvc(addFilters = false)
class PuzzleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        testUser = User.builder()
                .id("user-1")
                .username("testuser")
                .email("user@example.com")
                .build();

        principal = new UserDetailsImpl(
                testUser.getId(),
                testUser.getUsername(),
                testUser.getEmail(),
                "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // --- GET /api/puzzles/today ---

    @Nested
    @DisplayName("GET /api/puzzles/today Tests")
    class GetTodayPuzzleTests {

        @Test
        @DisplayName("Should return today's puzzle successfully with 200 OK")
        void shouldReturnTodayPuzzleSuccessfully() throws Exception {
            DailyPuzzleResponse response = DailyPuzzleResponse.builder()
                    .id("puz-1")
                    .puzzleDate(LocalDate.now())
                    .centerLetter("A")
                    .maxScore(100)
                    .outerLetters(Set.of("B", "C", "D", "E", "F", "G"))
                    .build();

            when(puzzleService.getTodayPuzzle()).thenReturn(response);

            mockMvc.perform(get("/api/puzzles/today"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("puz-1"))
                    .andExpect(jsonPath("$.centerLetter").value("A"))
                    .andExpect(jsonPath("$.maxScore").value(100));

            verify(puzzleService, times(1)).getTodayPuzzle();
        }

        @Test
        @DisplayName("GET /api/puzzles/today - Not Found 404")
        void getTodayPuzzle_NotFound() throws Exception {
            when(puzzleService.getTodayPuzzle())
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Puzzle del giorno non trovato"));

            mockMvc.perform(get("/api/puzzles/today"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Puzzle del giorno non trovato"));

            verify(puzzleService, times(1)).getTodayPuzzle();
        }
    }

    // --- POST /api/puzzles/{puzzleId}/submit ---

    @Nested
    @DisplayName("POST /api/puzzles/{puzzleId}/submit Tests")
    class SubmitWordTests {

        @Test
        @DisplayName("Should return 200 OK with valid word response")
        void shouldSubmitWordSuccessfully() throws Exception {
            WordSubmissionRequest request = new WordSubmissionRequest("ALBERGO");
            WordSubmissionResponse response = new WordSubmissionResponse(true, "ALBERGO", 14, true, null, null);

            when(puzzleService.validateAndScoreWord("user-1", "puz-1", "ALBERGO")).thenReturn(response);

            mockMvc.perform(post("/api/puzzles/puz-1/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.word").value("ALBERGO"))
                    .andExpect(jsonPath("$.score").value(14))
                    .andExpect(jsonPath("$.isMielegramma").value(true));

            verify(puzzleService, times(1)).validateAndScoreWord("user-1", "puz-1", "ALBERGO");
        }

        @Test
        @DisplayName("Should return 200 OK with invalid word response when missing center letter")
        void shouldReturnInvalidWordResponseWhenMissingCenterLetter() throws Exception {
            WordSubmissionRequest request = new WordSubmissionRequest("ROBO");
            WordSubmissionResponse response = new WordSubmissionResponse(
                    false, "ROBO", 0, false, ErrorTypeCode.MISSING_CENTER, "Manca la lettera centrale.");

            when(puzzleService.validateAndScoreWord("user-1", "puz-1", "ROBO")).thenReturn(response);

            mockMvc.perform(post("/api/puzzles/puz-1/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.word").value("ROBO"))
                    .andExpect(jsonPath("$.errorCode").value("MISSING_CENTER"))
                    .andExpect(jsonPath("$.errorMessage").value("Manca la lettera centrale."));

            verify(puzzleService, times(1)).validateAndScoreWord("user-1", "puz-1", "ROBO");
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when submitted puzzle ID does not exist")
        void shouldReturnNotFoundWhenPuzzleIdDoesNotExist() throws Exception {
            WordSubmissionRequest request = new WordSubmissionRequest("ALBERGO");

            when(puzzleService.validateAndScoreWord("user-1", "puz-invalid", "ALBERGO"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Puzzle non trovato"));

            mockMvc.perform(post("/api/puzzles/puz-invalid/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Puzzle non trovato"));

            verify(puzzleService, times(1)).validateAndScoreWord("user-1", "puz-invalid", "ALBERGO");
        }
    }

    @TestConfiguration
    static class TestConfig implements WebMvcConfigurer {
        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper;
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