package com.beesagono.backend.service;

import com.beesagono.backend.dto.auth.CreateAdminRequest;
import com.beesagono.backend.dto.auth.UserResponse;
import com.beesagono.backend.dto.dictionary.InvalidWordAttemptStatResponse;
import com.beesagono.backend.dto.puzzle.PuzzleAdminResponse;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleLettersRequest;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleWordsRequest;
import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.DictionaryWord;
import com.beesagono.backend.entity.InvalidWordAttempt;
import com.beesagono.backend.entity.PuzzleOuterLetter;
import com.beesagono.backend.entity.PuzzleWord;
import com.beesagono.backend.entity.Role;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.UserRole;
import com.beesagono.backend.entity.id.PuzzleOuterLetterId;
import com.beesagono.backend.entity.id.PuzzleWordId;
import com.beesagono.backend.enums.ErrorTypeCode;
import com.beesagono.backend.enums.RoleName;
import com.beesagono.backend.mapper.UserMapper;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import com.beesagono.backend.repository.DictionaryWordRepository;
import com.beesagono.backend.repository.GameSessionRepository;
import com.beesagono.backend.repository.InvalidWordAttemptRepository;
import com.beesagono.backend.repository.PuzzleWordRepository;
import com.beesagono.backend.repository.RoleRepository;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private DailyPuzzleRepository dailyPuzzleRepository;

    @Mock
    private InvalidWordAttemptRepository invalidWordAttemptRepository;

    @Mock
    private DictionaryWordRepository dictionaryWordRepository;

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private PuzzleWordRepository puzzleWordRepository;

    @Mock
    private PuzzleGeneratorService puzzleGeneratorService;

    @InjectMocks
    private AdminServiceImpl adminService;

    // --- User Management Tests ---

    @Test
    @DisplayName("createAdmin - Success")
    void shouldCreateAdminSuccessfully() {
        CreateAdminRequest request = createAdminRequest("adminuser", "admin@example.com", "password123");

        Role adminRole = createRole("role-admin", RoleName.ROLE_ADMIN);
        Role userRole = createRole("role-user", RoleName.ROLE_USER);

        User savedUser = createUser("user-admin-1", "adminuser", "admin@example.com");
        UserResponse expectedResponse = createUserResponse("user-admin-1", "adminuser", "admin@example.com", Set.of("ROLE_ADMIN", "ROLE_USER"));

        when(userRepository.existsByUsername("adminuser")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        when(userMapper.toUserResponse(savedUser)).thenReturn(expectedResponse);

        UserResponse response = adminService.createAdmin(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("user-admin-1");
        assertThat(response.getUsername()).isEqualTo("adminuser");

        verify(userRoleRepository, times(2)).save(any(UserRole.class));
        verify(userRepository, times(1)).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("createAdmin - Throws CONFLICT when username exists")
    void shouldThrowConflictWhenUsernameExists() {
        CreateAdminRequest request = createAdminRequest("existingAdmin", "admin@example.com", "pwd");

        when(userRepository.existsByUsername("existingAdmin")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createAdmin(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Username già in uso")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("createAdmin - Throws CONFLICT when email exists")
    void shouldThrowConflictWhenEmailExists() {
        CreateAdminRequest request = createAdminRequest("newAdmin", "existing@example.com", "pwd");

        when(userRepository.existsByUsername("newAdmin")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createAdmin(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email già in uso")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("createAdmin - Throws INTERNAL_SERVER_ERROR when ADMIN role missing")
    void shouldThrowInternalServerErrorWhenAdminRoleNotFound() {
        CreateAdminRequest request = createAdminRequest("adminuser", "admin@example.com", "pwd");

        when(userRepository.existsByUsername("adminuser")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.createAdmin(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ruolo ADMIN non trovato")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("getUsers - Returns page without search filter")
    void shouldGetUsersWithoutSearch() {
        Pageable pageable = Pageable.unpaged();
        User user = createUser("u1", "user1", "user1@example.com");
        UserResponse responseDto = createUserResponse("u1", "user1", "user1@example.com", Set.of("ROLE_USER"));

        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toUserResponse(user)).thenReturn(responseDto);

        Page<UserResponse> result = adminService.getUsers(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("user1");
        verify(userRepository, times(1)).findAll(pageable);
        verify(userRepository, never()).findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(any(), any(), any());
    }

    @Test
    @DisplayName("getUsers - Returns filtered page with search query")
    void shouldGetUsersWithSearch() {
        Pageable pageable = Pageable.unpaged();
        String searchKey = "john";

        User user = createUser("u2", "john_doe", "john@example.com");
        UserResponse responseDto = createUserResponse("u2", "john_doe", "john@example.com", Set.of("ROLE_USER"));

        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(searchKey, searchKey, pageable))
                .thenReturn(userPage);
        when(userMapper.toUserResponse(user)).thenReturn(responseDto);

        Page<UserResponse> result = adminService.getUsers(searchKey, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("john_doe");
        verify(userRepository, times(1)).findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                searchKey, searchKey, pageable);
        verify(userRepository, never()).findAll(pageable);
    }

    // --- Puzzle Management Tests ---

    @Test
    @DisplayName("generateOrResetFuturePuzzle - Success")
    void shouldGenerateOrResetFuturePuzzleSuccessfully() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        DailyPuzzle existingPuzzle = createDailyPuzzle("p1", futureDate, "A", null, null);
        DailyPuzzle newPuzzle = createDailyPuzzle("p2", futureDate, "B", null, null);

        when(dailyPuzzleRepository.findByPuzzleDate(futureDate)).thenReturn(Optional.of(existingPuzzle));
        when(puzzleGeneratorService.generateAndSavePuzzleForDate(futureDate)).thenReturn(newPuzzle);
        mockSessionCounts("p2", 2L, 5L);

        PuzzleAdminResponse response = adminService.generateOrResetFuturePuzzle(futureDate);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("p2");
        assertThat(response.getActiveSessionsCount()).isEqualTo(2L);
        assertThat(response.getCompletedSessionsCount()).isEqualTo(5L);

        verify(dailyPuzzleRepository, times(1)).delete(existingPuzzle);
        verify(puzzleGeneratorService, times(1)).generateAndSavePuzzleForDate(futureDate);
    }

    @Test
    @DisplayName("generateOrResetFuturePuzzle - Throws BAD_REQUEST for today or past date")
    void shouldThrowBadRequestForNonFutureDate() {
        LocalDate today = LocalDate.now();

        assertThatThrownBy(() -> adminService.generateOrResetFuturePuzzle(today))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Operazione consentita solo per i puzzle futuri")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(puzzleGeneratorService, never()).generateAndSavePuzzleForDate(any());
    }

    @Test
    @DisplayName("getPuzzleDetailsByDate - Success")
    void shouldGetPuzzleDetailsByDateSuccessfully() {
        LocalDate date = LocalDate.now();
        DailyPuzzle puzzle = createDailyPuzzle("p1", date, "A", null, null);

        when(dailyPuzzleRepository.findByPuzzleDate(date)).thenReturn(Optional.of(puzzle));
        mockSessionCounts("p1", 1L, 3L);

        PuzzleAdminResponse response = adminService.getPuzzleDetailsByDate(date);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("p1");
        assertThat(response.isEditable()).isFalse();
    }

    @Test
    @DisplayName("getPuzzleDetailsByDate - Throws NOT_FOUND when puzzle missing")
    void shouldThrowNotFoundWhenPuzzleMissing() {
        LocalDate date = LocalDate.now();
        when(dailyPuzzleRepository.findByPuzzleDate(date)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getPuzzleDetailsByDate(date))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Nessun puzzle trovato per la data")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("getAllPuzzlesOverview - Success")
    void shouldGetAllPuzzlesOverviewSuccessfully() {
        DailyPuzzle p1 = createDailyPuzzle("p1", LocalDate.now(), "A", null, null);
        DailyPuzzle p2 = createDailyPuzzle("p2", LocalDate.now().minusDays(1), "B", null, null);

        when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc()).thenReturn(List.of(p1, p2));
        mockSessionCounts("p1", 0L, 0L);
        mockSessionCounts("p2", 0L, 0L);

        List<PuzzleAdminResponse> responses = adminService.getAllPuzzlesOverview();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("p1");
        assertThat(responses.get(1).getId()).isEqualTo("p2");
    }

    @Test
    @DisplayName("updatePuzzleWords - Add and Remove words successfully")
    void shouldUpdatePuzzleWordsSuccessfully() {
        String puzzleId = "puzzle-1";
        LocalDate futureDate = LocalDate.now().plusDays(2);

        DictionaryWord oldWord = createDictionaryWord("CASA", 4, 3);
        DictionaryWord newWord = createDictionaryWord("ACERBA", 6, 5);

        PuzzleWord pw1 = createPuzzleWord(puzzleId, oldWord, false);
        List<PuzzleWord> puzzleWords = new ArrayList<>(List.of(pw1));

        List<PuzzleOuterLetter> outerLetters = List.of(
                createPuzzleOuterLetter(puzzleId, "C"),
                createPuzzleOuterLetter(puzzleId, "E"),
                createPuzzleOuterLetter(puzzleId, "R"),
                createPuzzleOuterLetter(puzzleId, "B"),
                createPuzzleOuterLetter(puzzleId, "S"),
                createPuzzleOuterLetter(puzzleId, "T")
        );

        DailyPuzzle puzzle = createDailyPuzzle(puzzleId, futureDate, "A", outerLetters, puzzleWords);
        UpdatePuzzleWordsRequest request = createUpdatePuzzleWordsRequest(Set.of("acerba"), Set.of("casa"));

        when(dailyPuzzleRepository.findById(puzzleId)).thenReturn(Optional.of(puzzle));
        when(dictionaryWordRepository.findById("ACERBA")).thenReturn(Optional.of(newWord));
        when(dailyPuzzleRepository.save(puzzle)).thenReturn(puzzle);
        mockSessionCounts(puzzleId, 0L, 0L);

        PuzzleAdminResponse response = adminService.updatePuzzleWords(puzzleId, request);

        assertThat(response).isNotNull();
        verify(puzzleWordRepository, times(1)).deleteAll(any());
        verify(puzzleWordRepository, times(1)).save(any(PuzzleWord.class));
        verify(dailyPuzzleRepository, times(1)).save(puzzle);
    }

    @Test
    @DisplayName("updatePuzzleWords - Throws BAD_REQUEST when added word is missing center letter")
    void shouldThrowBadRequestWhenWordMissingCenterLetter() {
        String puzzleId = "puzzle-1";
        DailyPuzzle puzzle = createDailyPuzzle(puzzleId, LocalDate.now().plusDays(2), "A", null, null);
        UpdatePuzzleWordsRequest request = createUpdatePuzzleWordsRequest(Set.of("TEST"), null);

        DictionaryWord dictWord = createDictionaryWord("TEST", 4, 3);

        when(dailyPuzzleRepository.findById(puzzleId)).thenReturn(Optional.of(puzzle));
        when(dictionaryWordRepository.findById("TEST")).thenReturn(Optional.of(dictWord));

        assertThatThrownBy(() -> adminService.updatePuzzleWords(puzzleId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("non contiene la lettera centrale obbligatoria")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("updatePuzzleLetters - Success")
    void shouldUpdatePuzzleLettersSuccessfully() {
        String puzzleId = "puzzle-1";
        DailyPuzzle puzzle = createDailyPuzzle(puzzleId, LocalDate.now().plusDays(2), "A", null, null);
        UpdatePuzzleLettersRequest request = createUpdatePuzzleLettersRequest("B", Set.of("C", "D", "E", "F", "G", "H"));

        when(dailyPuzzleRepository.findById(puzzleId)).thenReturn(Optional.of(puzzle));
        when(dailyPuzzleRepository.save(puzzle)).thenReturn(puzzle);
        mockSessionCounts(puzzleId, 0L, 0L);

        PuzzleAdminResponse response = adminService.updatePuzzleLetters(puzzleId, request);

        assertThat(response).isNotNull();
        verify(puzzleGeneratorService, times(1)).recalculatePuzzleWords(puzzle);
        verify(dailyPuzzleRepository, times(1)).save(puzzle);
    }

    // --- Audit & Dictionary Tests ---

    @Test
    @DisplayName("getTopSuggestedWordsFromAttempts - Success")
    void shouldGetTopSuggestedWordsFromAttemptsSuccessfully() {
        InvalidWordAttempt attempt1 = createInvalidWordAttempt("WORD1", ErrorTypeCode.NOT_IN_DICTIONARY);
        InvalidWordAttempt attempt2 = createInvalidWordAttempt("WORD1", ErrorTypeCode.NOT_IN_DICTIONARY);
        InvalidWordAttempt attempt3 = createInvalidWordAttempt("WORD2", ErrorTypeCode.NOT_IN_DICTIONARY);
        InvalidWordAttempt attempt4 = createInvalidWordAttempt("WORD3", ErrorTypeCode.TOO_SHORT);

        when(invalidWordAttemptRepository.findAll()).thenReturn(List.of(attempt1, attempt2, attempt3, attempt4));

        List<InvalidWordAttemptStatResponse> stats = adminService.getTopSuggestedWordsFromAttempts();

        assertThat(stats).hasSize(2);
        assertThat(stats.get(0).getWord()).isEqualTo("WORD1");
        assertThat(stats.get(0).getAttemptCount()).isEqualTo(2L);
        assertThat(stats.get(1).getWord()).isEqualTo("WORD2");
        assertThat(stats.get(1).getAttemptCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("removeWordFromDictionary - Success")
    void shouldRemoveWordFromDictionarySuccessfully() {
        String word = "casa";
        DictionaryWord dictWord = createDictionaryWord("CASA", 4, 3);

        when(dictionaryWordRepository.findById("CASA")).thenReturn(Optional.of(dictWord));

        adminService.removeWordFromDictionary(word);

        verify(puzzleWordRepository, times(1)).deleteByIdWord("CASA");
        verify(dictionaryWordRepository, times(1)).delete(dictWord);
    }

    @Test
    @DisplayName("removeWordFromDictionary - Throws NOT_FOUND when word not present")
    void shouldThrowNotFoundWhenWordNotPresentInDictionary() {
        String word = "UNKNOWN";
        when(dictionaryWordRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.removeWordFromDictionary(word))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Parola non trovata nel dizionario")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(puzzleWordRepository, never()).deleteByIdWord(any());
        verify(dictionaryWordRepository, never()).delete(any(DictionaryWord.class));
    }

    // --- Private Helper Methods ---

    private CreateAdminRequest createAdminRequest(String username, String email, String password) {
        CreateAdminRequest req = new CreateAdminRequest();
        req.setUsername(username);
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private UserResponse createUserResponse(String id, String username, String email, Set<String> roles) {
        return UserResponse.builder()
                .id(id)
                .username(username)
                .email(email)
                .roles(roles)
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

    private Role createRole(String id, RoleName name) {
        return Role.builder()
                .id(id)
                .name(name)
                .build();
    }

    private User createUser(String id, String username, String email) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .userRoles(new ArrayList<>())
                .build();
    }

    private DailyPuzzle createDailyPuzzle(String id, LocalDate date, String centerLetter,
                                          List<PuzzleOuterLetter> outerLetters, List<PuzzleWord> puzzleWords) {
        return DailyPuzzle.builder()
                .id(id)
                .puzzleDate(date)
                .centerLetter(centerLetter)
                .outerLetters(outerLetters != null ? outerLetters : new ArrayList<>())
                .puzzleWords(puzzleWords != null ? puzzleWords : new ArrayList<>())
                .maxScore(10)
                .build();
    }

    private DictionaryWord createDictionaryWord(String word, int length, int uniqueLettersCount) {
        return DictionaryWord.builder()
                .word(word)
                .wordLength(length)
                .uniqueLettersCount(uniqueLettersCount)
                .build();
    }

    private PuzzleWord createPuzzleWord(String puzzleId, DictionaryWord dictWord, boolean isMielegramma) {
        return PuzzleWord.builder()
                .id(new PuzzleWordId(puzzleId, dictWord.getWord()))
                .dictionaryWord(dictWord)
                .isMielegramma(isMielegramma)
                .build();
    }

    private PuzzleOuterLetter createPuzzleOuterLetter(String puzzleId, String letter) {
        return PuzzleOuterLetter.builder()
                .id(new PuzzleOuterLetterId(puzzleId, letter))
                .build();
    }

    private InvalidWordAttempt createInvalidWordAttempt(String word, ErrorTypeCode errorReason) {
        return InvalidWordAttempt.builder()
                .attemptedWord(word)
                .errorReason(errorReason)
                .build();
    }

    private void mockSessionCounts(String puzzleId, Long active, Long completed) {
        when(gameSessionRepository.countByPuzzleIdAndIsCompletedFalse(puzzleId)).thenReturn(active);
        when(gameSessionRepository.countByPuzzleIdAndIsCompletedTrue(puzzleId)).thenReturn(completed);
    }
}