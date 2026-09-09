package com.beesagono.backend.controller;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.dto.puzzle.WordSubmissionRequest;
import com.beesagono.backend.dto.puzzle.WordSubmissionResponse;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.security.JwtAuthenticationFilter;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.PuzzleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
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

    @Test
    @DisplayName("GET /api/puzzles/today - Success")
    void getTodayPuzzle_Success() throws Exception {
        DailyPuzzleResponse response = DailyPuzzleResponse.builder()
                .id("puz-1")
                .puzzleDate(LocalDate.now())
                .centerLetter("A")
                .maxScore(100)
                .outerLetters(Set.of("B", "C", "D", "E", "F", "G")) // <- Usa Set.of anziché List.of
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
    @DisplayName("POST /api/puzzles/{puzzleId}/submit - Success")
    void submitWord_Success() throws Exception {
        WordSubmissionRequest request = new WordSubmissionRequest("ALBERGO");
        WordSubmissionResponse response = new WordSubmissionResponse(true, "ALBERGO", 14, true, null, null);

        when(puzzleService.validateAndScoreWord("puz-1", "ALBERGO")).thenReturn(response);

        mockMvc.perform(post("/api/puzzles/puz-1/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.word").value("ALBERGO"))
                .andExpect(jsonPath("$.score").value(14))
                .andExpect(jsonPath("$.isMielegramma").value(true));

        verify(puzzleService, times(1)).validateAndScoreWord("puz-1", "ALBERGO");
    }
}