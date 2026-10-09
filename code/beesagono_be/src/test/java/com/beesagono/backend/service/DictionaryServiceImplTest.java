package com.beesagono.backend.service;

import com.beesagono.backend.dto.dictionary.AddWordRequest;
import com.beesagono.backend.dto.dictionary.BatchAddWordRequest;
import com.beesagono.backend.dto.dictionary.BatchUploadResponse;
import com.beesagono.backend.dto.dictionary.DictionaryFilterRequest;
import com.beesagono.backend.dto.dictionary.DictionaryWordResponse;
import com.beesagono.backend.dto.dictionary.WordValidationResponse;
import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.DictionaryWord;
import com.beesagono.backend.entity.PuzzleWord;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.mapper.DictionaryWordMapper;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import com.beesagono.backend.repository.DictionaryWordRepository;
import com.beesagono.backend.repository.PuzzleWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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

        lenient().when(scoringService.calculateWordScore(anyString(), anyBoolean()))
                .thenAnswer(invocation -> {
                    String word = invocation.getArgument(0);
                    boolean isMiele = invocation.getArgument(1);
                    if (word == null || word.length() < 4) return 0;
                    return word.length() + (isMiele ? 7 : 0);
                });
    }

    @Nested
    @DisplayName("addSingleWord Tests")
    class AddSingleWordTests {

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
    }

    @Nested
    @DisplayName("Batch Word Operations Tests")
    class BatchOperationsTests {

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
    }

    @Nested
    @DisplayName("getWords & validateWordForGuest Tests")
    class RetrievalAndValidationTests {

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

        @Test
        @DisplayName("validateWordForGuest - Valid word returns score and details")
        void shouldValidateWordForGuestSuccessfully() {
            String inputWord = "casa";
            LocalDate today = LocalDate.now();

            DailyPuzzle puzzle = DailyPuzzle.builder()
                    .id("puzzle-1")
                    .puzzleDate(today)
                    .centerLetter("A")
                    .build();

            PuzzleWord puzzleWord = PuzzleWord.builder()
                    .isMielegramma(false)
                    .build();

            when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.of(puzzle));
            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord("puzzle-1", "CASA"))
                    .thenReturn(Optional.of(puzzleWord));
            when(scoringService.calculateWordScore("CASA", false)).thenReturn(1);

            WordValidationResponse response = dictionaryService.validateWordForGuest(inputWord);

            assertThat(response.isValid()).isTrue();
            assertThat(response.getWord()).isEqualTo("CASA");
            assertThat(response.getPointsEarned()).isEqualTo(1);
            assertThat(response.isMielegramma()).isFalse();
        }

        @Test
        @DisplayName("validateWordForGuest - Returns invalid when word misses center letter")
        void shouldFailValidationWhenWordMissesCenterLetter() {
            String inputWord = "EDILIZIA";
            LocalDate today = LocalDate.now();

            DailyPuzzle puzzle = DailyPuzzle.builder()
                    .id("puzzle-1")
                    .puzzleDate(today)
                    .centerLetter("X")
                    .build();

            when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.of(puzzle));

            WordValidationResponse response = dictionaryService.validateWordForGuest(inputWord);

            assertThat(response.isValid()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("MISSING_CENTER");
        }
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