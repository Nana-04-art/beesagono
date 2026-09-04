package com.beesagono.backend.controller;

import com.beesagono.backend.dto.dictionary.*;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.security.JwtAuthenticationFilter;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.DictionaryService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DictionaryController.class)
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
    private JwtUtils jwtUtils;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private TokenBlacklist tokenBlacklist;

    private User adminUser;
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
        adminUser = User.builder()
                .id("admin-1")
                .username("admin")
                .email("admin@example.com")
                .build();

        principal = new UserDetailsImpl(
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.getEmail(),
                "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("POST /api/admin/dictionary/word - Success")
    void addSingleWord_Success() throws Exception {
        AddWordRequest request = new AddWordRequest();
        request.setWord("CASA");

        DictionaryWordResponse response = DictionaryWordResponse.builder()
                .word("CASA")
                .wordLength(4)
                .uniqueLettersCount(3)
                .isCandidatePangram(false)
                .build();

        when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));
        when(dictionaryService.addSingleWord(any(AddWordRequest.class), eq(adminUser))).thenReturn(response);

        mockMvc.perform(post("/api/admin/dictionary/word")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.word").value("CASA"))
                .andExpect(jsonPath("$.wordLength").value(4));

        verify(dictionaryService, times(1)).addSingleWord(any(AddWordRequest.class), eq(adminUser));
    }

    @Test
    @DisplayName("POST /api/admin/dictionary/words/batch - Success")
    void addBatchWords_Success() throws Exception {
        BatchAddWordRequest request = new BatchAddWordRequest();
        request.setWords(List.of("CASA", "ALBERO"));

        BatchUploadResponse response = BatchUploadResponse.builder()
                .totalProcessed(2)
                .addedCount(2)
                .skippedCount(0)
                .message("Inserite 2 parole.")
                .build();

        when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));
        when(dictionaryService.addBatchWords(any(BatchAddWordRequest.class), eq(adminUser))).thenReturn(response);

        mockMvc.perform(post("/api/admin/dictionary/words/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalProcessed").value(2))
                .andExpect(jsonPath("$.addedCount").value(2));

        verify(dictionaryService, times(1)).addBatchWords(any(BatchAddWordRequest.class), eq(adminUser));
    }

    @Test
    @DisplayName("POST /api/admin/dictionary/upload - Success")
    void uploadFromFile_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "words.txt", MediaType.TEXT_PLAIN_VALUE,
                "casa\nalbero".getBytes());

        BatchUploadResponse response = BatchUploadResponse.builder()
                .totalProcessed(2)
                .addedCount(2)
                .skippedCount(0)
                .message("Inserite 2 parole.")
                .build();

        when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));
        when(dictionaryService.uploadWordsFromFile(any(), eq(adminUser))).thenReturn(response);

        mockMvc.perform(multipart("/api/admin/dictionary/upload")
                .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addedCount").value(2));

        verify(dictionaryService, times(1)).uploadWordsFromFile(any(), eq(adminUser));
    }

    @Test
    @DisplayName("GET /api/admin/dictionary - Success")
    void getWords_Success() throws Exception {
        DictionaryWordResponse wordResponse = DictionaryWordResponse.builder().word("CASA").build();
        Page<DictionaryWordResponse> page = new PageImpl<>(List.of(wordResponse));

        when(dictionaryService.getWords(any(DictionaryFilterRequest.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/dictionary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].word").value("CASA"));

        verify(dictionaryService, times(1)).getWords(any(DictionaryFilterRequest.class), any(Pageable.class));
    }

    @Test
    @DisplayName("POST /api/admin/dictionary/word - Admin Not Found Throws 404")
    void addSingleWord_AdminNotFound() throws Exception {
        AddWordRequest request = new AddWordRequest();
        request.setWord("CASA");

        when(userRepository.findById("admin-1")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/dictionary/word")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}