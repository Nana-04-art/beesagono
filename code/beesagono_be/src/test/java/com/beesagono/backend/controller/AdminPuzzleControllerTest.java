package com.beesagono.backend.controller;

import com.beesagono.backend.dto.puzzle.PuzzleAdminResponse;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleLettersRequest;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleWordsRequest;
import com.beesagono.backend.security.GlobalExceptionHandler;
import com.beesagono.backend.security.JwtAuthenticationFilter;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@WebMvcTest(AdminPuzzleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminPuzzleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private TokenBlacklist tokenBlacklist;

    private PuzzleAdminResponse mockPuzzleResponse;

    @BeforeEach
    void setUp() {
        UserDetailsImpl principal = createAdminPrincipal("admin-1", "admin", "admin@example.com");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockPuzzleResponse = createPuzzleAdminResponse("puzzle-123", LocalDate.of(2026, 10, 15), "A");
    }

    // --- POST /api/admin/puzzle/generate ---

    @Test
    @DisplayName("POST /generate - Success")
    void generatePuzzle_Success() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 15);
        Mockito.when(this.adminService.generateOrResetFuturePuzzle(date)).thenReturn(this.mockPuzzleResponse);

        this.mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/puzzle/generate")
                .param("date", "2026-10-15"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

        Mockito.verify(this.adminService, Mockito.times(1)).generateOrResetFuturePuzzle(date);
    }

    @Test
    @DisplayName("POST /generate - Missing Param (400 Bad Request)")
    void generatePuzzle_MissingDateParam() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/puzzle/generate"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        Mockito.verifyNoInteractions(this.adminService);
    }

    // --- GET /api/admin/puzzle ---

    @Test
    @DisplayName("GET / - Success")
    void getPuzzleByDate_Success() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 15);
        Mockito.when(this.adminService.getPuzzleDetailsByDate(date)).thenReturn(this.mockPuzzleResponse);

        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/puzzle")
                .param("date", "2026-10-15"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

        Mockito.verify(this.adminService, Mockito.times(1)).getPuzzleDetailsByDate(date);
    }

    // --- GET /api/admin/puzzle/all ---

    @Test
    @DisplayName("GET /all - Success")
    void getAllPuzzles_Success() throws Exception {
        Mockito.when(this.adminService.getAllPuzzlesOverview()).thenReturn(List.of(this.mockPuzzleResponse));

        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/puzzle/all"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1));

        Mockito.verify(this.adminService, Mockito.times(1)).getAllPuzzlesOverview();
    }

    // --- PATCH /api/admin/puzzle/{puzzleId}/words ---

    @Test
    @DisplayName("PATCH /{puzzleId}/words - Success")
    void updatePuzzleWords_Success() throws Exception {
        String puzzleId = "puzzle-123";
        UpdatePuzzleWordsRequest request = createUpdatePuzzleWordsRequest(Set.of("CASA"), Set.of("PANE"));

        Mockito.when(this.adminService.updatePuzzleWords(ArgumentMatchers.eq(puzzleId), ArgumentMatchers.any(UpdatePuzzleWordsRequest.class)))
                .thenReturn(this.mockPuzzleResponse);

        this.mockMvc.perform(MockMvcRequestBuilders.patch("/api/admin/puzzle/{puzzleId}/words", puzzleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(this.adminService, Mockito.times(1))
                .updatePuzzleWords(ArgumentMatchers.eq(puzzleId), ArgumentMatchers.any(UpdatePuzzleWordsRequest.class));
    }

    // --- PUT /api/admin/puzzle/{puzzleId} ---

    @Test
    @DisplayName("PUT /{puzzleId} - Success")
    void updatePuzzleLetters_Success() throws Exception {
        String puzzleId = "puzzle-123";
        UpdatePuzzleLettersRequest request = createUpdatePuzzleLettersRequest("A", Set.of("B", "C", "D", "E", "F", "G"));

        Mockito.when(this.adminService.updatePuzzleLetters(ArgumentMatchers.eq(puzzleId), ArgumentMatchers.any(UpdatePuzzleLettersRequest.class)))
                .thenReturn(this.mockPuzzleResponse);

        this.mockMvc.perform(MockMvcRequestBuilders.put("/api/admin/puzzle/{puzzleId}", puzzleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(this.adminService, Mockito.times(1))
                .updatePuzzleLetters(ArgumentMatchers.eq(puzzleId), ArgumentMatchers.any(UpdatePuzzleLettersRequest.class));
    }

    // --- Private Helper Methods ---

    private UserDetailsImpl createAdminPrincipal(String id, String username, String email) {
        return new UserDetailsImpl(
                id, username, email, "pwd", 
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private PuzzleAdminResponse createPuzzleAdminResponse(String id, LocalDate date, String centerLetter) {
        return PuzzleAdminResponse.builder()
                .id(id)
                .puzzleDate(date)
                .centerLetter(centerLetter)
                .outerLetters(Set.of("B", "C", "D", "E", "F", "G"))
                .maxScore(100)
                .editable(true)
                .validWords(List.of("CASA", "PANE"))
                .activeSessionsCount(10L)
                .completedSessionsCount(2L)
                .build();
    }

    private UpdatePuzzleWordsRequest createUpdatePuzzleWordsRequest(Set<String> wordsToAdd, Set<String> wordsToRemove) {
        UpdatePuzzleWordsRequest request = new UpdatePuzzleWordsRequest();
        request.setWordsToAdd(wordsToAdd);
        request.setWordsToRemove(wordsToRemove);
        return request;
    }

    private UpdatePuzzleLettersRequest createUpdatePuzzleLettersRequest(String centerLetter, Set<String> outerLetters) {
        UpdatePuzzleLettersRequest request = new UpdatePuzzleLettersRequest();
        request.setCenterLetter(centerLetter);
        request.setOuterLetters(outerLetters);
        return request;
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
                public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                    return SecurityContextHolder.getContext().getAuthentication() != null 
                            ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() 
                            : null;
                }
            });
        }
    }
}