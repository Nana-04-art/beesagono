package com.beesagono.backend.controller;

import com.beesagono.backend.dto.dictionary.AddWordRequest;
import com.beesagono.backend.dto.dictionary.BatchAddWordRequest;
import com.beesagono.backend.dto.dictionary.BatchUploadResponse;
import com.beesagono.backend.dto.dictionary.DictionaryFilterRequest;
import com.beesagono.backend.dto.dictionary.DictionaryWordResponse;
import com.beesagono.backend.dto.dictionary.InvalidWordAttemptStatResponse;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.security.GlobalExceptionHandler;
import com.beesagono.backend.security.JwtAuthenticationFilter;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.AdminService;
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
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DictionaryController.class)
@Import(GlobalExceptionHandler.class)
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
    private AdminService adminService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private TokenBlacklist tokenBlacklist;

    private User adminUser;
    private UserDetailsImpl principal;

    @BeforeEach
    void setUp() {
        adminUser = createAdminUser("admin-1", "admin", "admin@example.com");
        principal = createAdminPrincipal(adminUser);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // --- POST /api/admin/dictionary/word ---

    @Test
    @DisplayName("POST /api/admin/dictionary/word - Success")
    void addSingleWord_Success() throws Exception {
        AddWordRequest request = createAddWordRequest("CASA");
        DictionaryWordResponse response = createDictionaryWordResponse("CASA", 4, 3, false);

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
    @DisplayName("POST /api/admin/dictionary/word - Admin Not Found Throws 404")
    void addSingleWord_AdminNotFound() throws Exception {
        AddWordRequest request = createAddWordRequest("CASA");

        when(userRepository.findById("admin-1")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/dictionary/word")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Utente admin non trovato."));
    }

    @Test
    @DisplayName("POST /api/admin/dictionary/word - Word Already Exists Throws 409")
    void addSingleWord_Conflict() throws Exception {
        AddWordRequest request = createAddWordRequest("CASA");

        when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));
        when(dictionaryService.addSingleWord(any(AddWordRequest.class), eq(adminUser)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Parola già presente nel dizionario"));

        mockMvc.perform(post("/api/admin/dictionary/word")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Parola già presente nel dizionario"));
    }

    // --- POST /api/admin/dictionary/words/batch ---

    @Test
    @DisplayName("POST /api/admin/dictionary/words/batch - Success")
    void addBatchWords_Success() throws Exception {
        BatchAddWordRequest request = createBatchAddWordRequest(List.of("CASA", "ALBERO"));
        BatchUploadResponse response = createBatchUploadResponse(2, 2, 0, "Inserite 2 parole.");

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
    @DisplayName("POST /api/admin/dictionary/words/batch - Admin Not Found Throws 404")
    void addBatchWords_AdminNotFound() throws Exception {
        BatchAddWordRequest request = createBatchAddWordRequest(List.of("CASA", "ALBERO"));

        when(userRepository.findById("admin-1")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/dictionary/words/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- POST /api/admin/dictionary/upload ---

    @Test
    @DisplayName("POST /api/admin/dictionary/upload - Success")
    void uploadFromFile_Success() throws Exception {
        MockMultipartFile file = createMockMultipartFile("words.txt", MediaType.TEXT_PLAIN_VALUE, "casa\nalbero".getBytes());
        BatchUploadResponse response = createBatchUploadResponse(2, 2, 0, "Inserite 2 parole.");

        when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));
        when(dictionaryService.uploadWordsFromFile(any(), eq(adminUser))).thenReturn(response);

        mockMvc.perform(multipart("/api/admin/dictionary/upload")
                .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addedCount").value(2));

        verify(dictionaryService, times(1)).uploadWordsFromFile(any(), eq(adminUser));
    }

    @Test
    @DisplayName("POST /api/admin/dictionary/upload - Invalid File Format Throws 400")
    void uploadFromFile_BadRequest() throws Exception {
        MockMultipartFile file = createMockMultipartFile("image.png", MediaType.IMAGE_PNG_VALUE, new byte[0]);

        when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));
        when(dictionaryService.uploadWordsFromFile(any(), eq(adminUser)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato file non supportato"));

        mockMvc.perform(multipart("/api/admin/dictionary/upload")
                .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Formato file non supportato"));
    }

    // --- GET /api/admin/dictionary ---

    @Test
    @DisplayName("GET /api/admin/dictionary - Success")
    void getWords_Success() throws Exception {
        DictionaryWordResponse wordResponse = createDictionaryWordResponse("CASA", 4, 3, false);
        Page<DictionaryWordResponse> page = new PageImpl<>(List.of(wordResponse));

        when(dictionaryService.getWords(any(DictionaryFilterRequest.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/dictionary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].word").value("CASA"));

        verify(dictionaryService, times(1)).getWords(any(DictionaryFilterRequest.class), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/admin/dictionary - Internal Server Error Throws 500")
    void getWords_InternalServerError() throws Exception {
        when(dictionaryService.getWords(any(DictionaryFilterRequest.class), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/admin/dictionary"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Si è verificato un errore interno al server."));
    }

    // --- GET /api/admin/dictionary/invalid-attempts ---

    @Test
    @DisplayName("GET /api/admin/dictionary/invalid-attempts - Success")
    void getTopSuggestedWords_Success() throws Exception {
        InvalidWordAttemptStatResponse statResponse = createInvalidAttemptStatResponse("ERRATA", 5L);

        when(adminService.getTopSuggestedWordsFromAttempts()).thenReturn(List.of(statResponse));

        mockMvc.perform(get("/api/admin/dictionary/invalid-attempts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].word").value("ERRATA"))
                .andExpect(jsonPath("$[0].attemptCount").value(5));

        verify(adminService, times(1)).getTopSuggestedWordsFromAttempts();
    }

    @Test
    @DisplayName("GET /api/admin/dictionary/invalid-attempts - Internal Server Error Throws 500")
    void getTopSuggestedWords_InternalServerError() throws Exception {
        when(adminService.getTopSuggestedWordsFromAttempts())
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/admin/dictionary/invalid-attempts"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    // --- DELETE /api/admin/dictionary/word/{word} ---

    @Test
    @DisplayName("DELETE /api/admin/dictionary/word/{word} - Success")
    void removeWordFromDictionary_Success() throws Exception {
        mockMvc.perform(delete("/api/admin/dictionary/word/CASA"))
                .andExpect(status().isNoContent());

        verify(adminService, times(1)).removeWordFromDictionary("CASA");
    }

    @Test
    @DisplayName("DELETE /api/admin/dictionary/word/{word} - Word Not Found Throws 404")
    void removeWordFromDictionary_NotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Parola non trovata nel dizionario"))
                .when(adminService).removeWordFromDictionary("INESISTENTE");

        mockMvc.perform(delete("/api/admin/dictionary/word/INESISTENTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Parola non trovata nel dizionario"));

        verify(adminService, times(1)).removeWordFromDictionary("INESISTENTE");
    }

    // --- Private Helper Methods ---

    private User createAdminUser(String id, String username, String email) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .build();
    }

    private UserDetailsImpl createAdminPrincipal(User user) {
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private AddWordRequest createAddWordRequest(String word) {
        AddWordRequest request = new AddWordRequest();
        request.setWord(word);
        return request;
    }

    private BatchAddWordRequest createBatchAddWordRequest(List<String> words) {
        BatchAddWordRequest request = new BatchAddWordRequest();
        request.setWords(words);
        return request;
    }

    private DictionaryWordResponse createDictionaryWordResponse(String word, int length, int uniqueLetters, boolean isPangram) {
        return DictionaryWordResponse.builder()
                .word(word)
                .wordLength(length)
                .uniqueLettersCount(uniqueLetters)
                .isCandidatePangram(isPangram)
                .build();
    }

    private BatchUploadResponse createBatchUploadResponse(int total, int added, int skipped, String message) {
        return BatchUploadResponse.builder()
                .totalProcessed(total)
                .addedCount(added)
                .skippedCount(skipped)
                .message(message)
                .build();
    }

    private MockMultipartFile createMockMultipartFile(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("file", filename, contentType, content);
    }

    private InvalidWordAttemptStatResponse createInvalidAttemptStatResponse(String word, long count) {
        return new InvalidWordAttemptStatResponse(word, count);
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