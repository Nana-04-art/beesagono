package com.beesagono.backend.service;

import com.beesagono.backend.dto.dictionary.AddWordRequest;
import com.beesagono.backend.dto.dictionary.BatchAddWordRequest;
import com.beesagono.backend.dto.dictionary.BatchUploadResponse;
import com.beesagono.backend.dto.dictionary.DictionaryFilterRequest;
import com.beesagono.backend.dto.dictionary.DictionaryWordResponse;
import com.beesagono.backend.dto.dictionary.WordValidationResponse;
import com.beesagono.backend.entity.*;
import com.beesagono.backend.entity.id.PuzzleOuterLetterId;
import com.beesagono.backend.mapper.DictionaryWordMapper;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import com.beesagono.backend.repository.DictionaryWordRepository;
import com.beesagono.backend.repository.PuzzleWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceImplTest {

    @Mock
    private DictionaryWordRepository dictionaryWordRepository;

    @Mock
    private DailyPuzzleRepository dailyPuzzleRepository;

    @Mock
    private PuzzleWordRepository puzzleWordRepository;

    @Mock
    private DictionaryWordMapper dictionaryWordMapper;

    @Mock
    private ScoringService scoringService;

    @InjectMocks
    private DictionaryServiceImpl dictionaryService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().id("admin-1").username("admin").build();
    }

    @Test
    @DisplayName("addSingleWord - Success")
    void shouldAddSingleWordSuccessfully() {
        AddWordRequest request = createAddWordRequest("àlbero", null);

        DictionaryWord savedWord = createDictionaryWord("ALBERO", 6, 5, false, adminUser);
        DictionaryWordResponse expectedResponse = createDictionaryWordResponse("ALBERO", 6, 5, false);

        when(dictionaryWordRepository.existsById("ALBERO")).thenReturn(false);
        when(dictionaryWordRepository.save(any(DictionaryWord.class))).thenReturn(savedWord);
        when(dictionaryWordMapper.toDictionaryWordResponse(savedWord)).thenReturn(expectedResponse);

        DictionaryWordResponse response = dictionaryService.addSingleWord(request, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.getWord()).isEqualTo("ALBERO");
        verify(dictionaryWordRepository, times(1)).save(any(DictionaryWord.class));
    }

    @Test
    @DisplayName("addSingleWord - Throws Exception when word length < 4")
    void shouldThrowExceptionWhenWordTooShort() {
        AddWordRequest request = createAddWordRequest("SOL", null);

        assertThatThrownBy(() -> dictionaryService.addSingleWord(request, adminUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("almeno 4 lettere")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(dictionaryWordRepository, never()).save(any());
    }

    @Test
    @DisplayName("addSingleWord - Throws Exception when word already exists")
    void shouldThrowExceptionWhenWordAlreadyExists() {
        AddWordRequest request = createAddWordRequest("CASA", null);

        when(dictionaryWordRepository.existsById("CASA")).thenReturn(true);

        assertThatThrownBy(() -> dictionaryService.addSingleWord(request, adminUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("già presente nel dizionario")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(dictionaryWordRepository, never()).save(any());
    }

    @Test
    @DisplayName("addSingleWord - Throws Exception when unique letters > 7")
    void shouldThrowExceptionWhenTooManyUniqueLetters() {
        AddWordRequest request = createAddWordRequest("ABCDEFGHI", null);

        assertThatThrownBy(() -> dictionaryService.addSingleWord(request, adminUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("più di 7 lettere uniche")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(dictionaryWordRepository, never()).save(any());
    }

    @Test
    @DisplayName("addBatchWords - Success")
    void shouldAddBatchWordsSuccessfully() {
        BatchAddWordRequest request = new BatchAddWordRequest();
        request.setWords(List.of("casa", "albero", "duplicata"));

        DictionaryWord duplicateEntity = createDictionaryWord("DUPLICATA", 9, 7, true, adminUser);
        when(dictionaryWordRepository.findAllById(any())).thenReturn(List.of(duplicateEntity));

        BatchUploadResponse response = dictionaryService.addBatchWords(request, adminUser);

        assertThat(response.getTotalProcessed()).isEqualTo(3);
        assertThat(response.getAddedCount()).isEqualTo(2);
        assertThat(response.getSkippedCount()).isEqualTo(1);
        verify(dictionaryWordRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("uploadWordsFromFile - Success")
    void shouldUploadWordsFromFileSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "casa, albero\nfiore".getBytes());

        when(dictionaryWordRepository.findAllById(any())).thenReturn(Collections.emptyList());

        BatchUploadResponse response = dictionaryService.uploadWordsFromFile(file, adminUser);

        assertThat(response.getTotalProcessed()).isEqualTo(3);
        assertThat(response.getAddedCount()).isEqualTo(3);
        verify(dictionaryWordRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("uploadWordsFromFile - Empty file throws Exception")
    void shouldThrowExceptionWhenUploadedFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> dictionaryService.uploadWordsFromFile(file, adminUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("file inviato è vuoto")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("getWords - Success")
    @SuppressWarnings("unchecked")
    void shouldGetWordsSuccessfully() {
        DictionaryFilterRequest filterRequest = new DictionaryFilterRequest();
        Pageable pageable = Pageable.unpaged();

        DictionaryWord entity = createDictionaryWord("CASA", 4, 3, false, adminUser);
        DictionaryWordResponse responseDto = createDictionaryWordResponse("CASA", 4, 3, false);
        Page<DictionaryWord> page = new PageImpl<>(List.of(entity));

        when(dictionaryWordRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(dictionaryWordMapper.toDictionaryWordResponse(entity)).thenReturn(responseDto);

        Page<DictionaryWordResponse> result = dictionaryService.getWords(filterRequest, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getWord()).isEqualTo("CASA");
    }

    // --- validateWordForGuest Tests ---

    @Test
    @DisplayName("validateWordForGuest - Success")
    void validateWordForGuest_Success() {
        String puzzleDateStr = "2026-10-02";
        LocalDate puzzleDate = LocalDate.parse(puzzleDateStr);
        String rawWord = "casa";
        String cleanWord = "CASA";

        PuzzleOuterLetterId idC = new PuzzleOuterLetterId();
        idC.setLetter("C");

        PuzzleOuterLetterId idS = new PuzzleOuterLetterId();
        idS.setLetter("S");

        PuzzleOuterLetter letter1 = new PuzzleOuterLetter();
        letter1.setId(idC);

        PuzzleOuterLetter letter2 = new PuzzleOuterLetter();
        letter2.setId(idS);

        DailyPuzzle puzzle = DailyPuzzle.builder()
                .id("puzzle-1")
                .puzzleDate(puzzleDate)
                .centerLetter("A")
                .outerLetters(List.of(letter1, letter2))
                .build();

        PuzzleWord puzzleWord = PuzzleWord.builder()
                .isMielegramma(false)
                .build();

        when(dailyPuzzleRepository.findByPuzzleDate(puzzleDate)).thenReturn(Optional.of(puzzle));
        when(puzzleWordRepository.findByIdPuzzleIdAndIdWord("puzzle-1", cleanWord)).thenReturn(Optional.of(puzzleWord));
        when(scoringService.calculateWordScore(cleanWord, false)).thenReturn(1);

        WordValidationResponse response = dictionaryService.validateWordForGuest(puzzleDateStr, rawWord);

        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
        assertThat(response.getWord()).isEqualTo(cleanWord);
        assertThat(response.getPointsEarned()).isEqualTo(1);
        assertThat(response.isMielegramma()).isFalse();
    }

    @Test
    @DisplayName("validateWordForGuest - Empty or null word returns TOO_SHORT error")
    void validateWordForGuest_EmptyWord() {
        WordValidationResponse response = dictionaryService.validateWordForGuest("2026-10-02", "   ");

        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("TOO_SHORT");
        assertThat(response.getErrorMessage()).contains("non può essere vuota");
    }

    @Test
    @DisplayName("validateWordForGuest - Word shorter than 4 letters returns TOO_SHORT error")
    void validateWordForGuest_WordTooShort() {
        String puzzleDateStr = "2026-10-02";
        LocalDate puzzleDate = LocalDate.parse(puzzleDateStr);

        DailyPuzzle puzzle = DailyPuzzle.builder()
                .id("puzzle-1")
                .puzzleDate(puzzleDate)
                .centerLetter("A")
                .build();

        when(dailyPuzzleRepository.findByPuzzleDate(puzzleDate)).thenReturn(Optional.of(puzzle));

        WordValidationResponse response = dictionaryService.validateWordForGuest(puzzleDateStr, "sol");

        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("TOO_SHORT");
        assertThat(response.getErrorMessage()).contains("almeno 4 lettere");
    }

    @Test
    @DisplayName("validateWordForGuest - Missing center letter returns MISSING_CENTER error")
    void validateWordForGuest_MissingCenterLetter() {
        String puzzleDateStr = "2026-10-02";
        LocalDate puzzleDate = LocalDate.parse(puzzleDateStr);

        DailyPuzzle puzzle = DailyPuzzle.builder()
                .id("puzzle-1")
                .puzzleDate(puzzleDate)
                .centerLetter("Z")
                .build();

        when(dailyPuzzleRepository.findByPuzzleDate(puzzleDate)).thenReturn(Optional.of(puzzle));

        WordValidationResponse response = dictionaryService.validateWordForGuest(puzzleDateStr, "casa");

        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("MISSING_CENTER");
        assertThat(response.getErrorMessage()).contains("lettera centrale obbligatoria");
    }

    @Test
    @DisplayName("validateWordForGuest - Word not in puzzle dictionary returns NOT_IN_DICTIONARY error")
    void validateWordForGuest_NotInDictionary() {
        String puzzleDateStr = "2026-10-02";
        LocalDate puzzleDate = LocalDate.parse(puzzleDateStr);

        PuzzleOuterLetterId idC = new PuzzleOuterLetterId();
        idC.setLetter("C");

        PuzzleOuterLetterId idS = new PuzzleOuterLetterId();
        idS.setLetter("S");

        PuzzleOuterLetter letter1 = new PuzzleOuterLetter();
        letter1.setId(idC);

        PuzzleOuterLetter letter2 = new PuzzleOuterLetter();
        letter2.setId(idS);

        DailyPuzzle puzzle = DailyPuzzle.builder()
                .id("puzzle-1")
                .puzzleDate(puzzleDate)
                .centerLetter("A")
                .outerLetters(List.of(letter1, letter2))
                .build();

        when(dailyPuzzleRepository.findByPuzzleDate(puzzleDate)).thenReturn(Optional.of(puzzle));
        when(puzzleWordRepository.findByIdPuzzleIdAndIdWord("puzzle-1", "CASA")).thenReturn(Optional.empty());

        WordValidationResponse response = dictionaryService.validateWordForGuest(puzzleDateStr, "casa");

        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("NOT_IN_DICTIONARY");
        assertThat(response.getErrorMessage()).contains("dizionario ufficiale");
    }

    @Test
    @DisplayName("validateWordForGuest - Throws 404 NOT_FOUND when puzzle is missing for date")
    void validateWordForGuest_PuzzleNotFound() {
        String puzzleDateStr = "2026-10-02";
        LocalDate puzzleDate = LocalDate.parse(puzzleDateStr);

        when(dailyPuzzleRepository.findByPuzzleDate(puzzleDate)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dictionaryService.validateWordForGuest(puzzleDateStr, "casa"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Puzzle non trovato")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- Helper Methods ---

    private AddWordRequest createAddWordRequest(String word, Boolean isCandidatePangram) {
        AddWordRequest request = new AddWordRequest();
        request.setWord(word);
        request.setIsCandidatePangram(isCandidatePangram);
        return request;
    }

    private DictionaryWord createDictionaryWord(String word, int length, int uniqueLetters, boolean isPangram, User user) {
        return DictionaryWord.builder()
                .word(word)
                .wordLength(length)
                .uniqueLettersCount(uniqueLetters)
                .isCandidatePangram(isPangram)
                .addedByUser(user)
                .build();
    }

    private DictionaryWordResponse createDictionaryWordResponse(String word, int length, int uniqueLetters, boolean isPangram) {
        return DictionaryWordResponse.builder()
                .word(word)
                .wordLength(length)
                .uniqueLettersCount(uniqueLetters)
                .isCandidatePangram(isPangram)
                .build();
    }
}