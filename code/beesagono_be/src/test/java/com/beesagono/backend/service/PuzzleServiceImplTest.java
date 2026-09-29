package com.beesagono.backend.service;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.dto.puzzle.WordSubmissionResponse;
import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.PuzzleWord;
import com.beesagono.backend.enums.ErrorTypeCode;
import com.beesagono.backend.mapper.DailyPuzzleMapper;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import com.beesagono.backend.repository.GameSessionRepository;
import com.beesagono.backend.repository.PuzzleWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PuzzleServiceImplTest {

    @Mock
    private DailyPuzzleRepository dailyPuzzleRepository;

    @Mock
    private PuzzleWordRepository puzzleWordRepository;

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private PuzzleGeneratorService puzzleGeneratorService;

    @Mock
    private DailyPuzzleMapper dailyPuzzleMapper;

    @Mock
    private ScoringService scoringService;

    @InjectMocks
    private PuzzleServiceImpl puzzleService;

    private DailyPuzzle samplePuzzle;
    private DailyPuzzleResponse sampleResponse;
    private final String userId = "user-123";

    @BeforeEach
    void setUp() {
        samplePuzzle = DailyPuzzle.builder()
                .id("puz-123")
                .puzzleDate(LocalDate.now())
                .centerLetter("A")
                .maxScore(100)
                .build();

        sampleResponse = DailyPuzzleResponse.builder()
                .id("puz-123")
                .puzzleDate(LocalDate.now())
                .centerLetter("A")
                .maxScore(100)
                .build();
    }

    // --- getTodayPuzzle ---
    @Nested
    @DisplayName("getTodayPuzzle - Puzzle Retrieval & Generation")
    class GetTodayPuzzleTests {

        @Test
        @DisplayName("Should generate puzzle if missing and return mapped DTO")
        void shouldGeneratePuzzleIfNotExistsAndReturnResponse() {
            LocalDate today = LocalDate.now();

            when(dailyPuzzleRepository.existsByPuzzleDate(today)).thenReturn(false);
            when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.of(samplePuzzle));
            when(dailyPuzzleMapper.toDailyPuzzleResponse(samplePuzzle)).thenReturn(sampleResponse);

            DailyPuzzleResponse response = puzzleService.getTodayPuzzle();

            verify(puzzleGeneratorService, times(1)).generateAndSavePuzzleForDate(today);
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo("puz-123");
        }

        @Test
        @DisplayName("Should use existing puzzle without triggering generation")
        void shouldReturnExistingPuzzleWithoutGeneration() {
            LocalDate today = LocalDate.now();

            when(dailyPuzzleRepository.existsByPuzzleDate(today)).thenReturn(true);
            when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.of(samplePuzzle));
            when(dailyPuzzleMapper.toDailyPuzzleResponse(samplePuzzle)).thenReturn(sampleResponse);

            DailyPuzzleResponse response = puzzleService.getTodayPuzzle();

            verify(puzzleGeneratorService, never()).generateAndSavePuzzleForDate(any());
            assertThat(response).isEqualTo(sampleResponse);
        }

        @Test
        @DisplayName("Should catch DataIntegrityViolationException and fetch existing puzzle during concurrent creation")
        void shouldHandleConcurrentPuzzleGeneration() {
            LocalDate today = LocalDate.now();

            when(dailyPuzzleRepository.existsByPuzzleDate(today)).thenReturn(false);
            doThrow(new DataIntegrityViolationException("Duplicate key"))
                    .when(puzzleGeneratorService).generateAndSavePuzzleForDate(today);
            when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.of(samplePuzzle));
            when(dailyPuzzleMapper.toDailyPuzzleResponse(samplePuzzle)).thenReturn(sampleResponse);

            DailyPuzzleResponse response = puzzleService.getTodayPuzzle();

            verify(puzzleGeneratorService, times(1)).generateAndSavePuzzleForDate(today);
            assertThat(response).isEqualTo(sampleResponse);
        }

        @Test
        @DisplayName("Should throw ResponseStatusException NOT_FOUND when puzzle is missing after generation attempt")
        void shouldThrowExceptionWhenPuzzleNotFound() {
            LocalDate today = LocalDate.now();

            when(dailyPuzzleRepository.existsByPuzzleDate(today)).thenReturn(false);
            when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> puzzleService.getTodayPuzzle())
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Puzzle del giorno non trovato");
        }
    }

    @Nested
    @DisplayName("validateAndScoreWord - Word Submission & Validation")
    class ValidateAndScoreWordTests {

        @Test
        @DisplayName("Should throw ResponseStatusException NOT_FOUND when puzzleId does not exist")
        void shouldThrowExceptionWhenPuzzleNotFound() {
            when(dailyPuzzleRepository.findById("invalid-puz")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> puzzleService.validateAndScoreWord(userId, "invalid-puz", "CASA"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Puzzle non trovato con ID: invalid-puz");
        }

        @Test
        @DisplayName("Should reject null, blank, or words shorter than 4 letters")
        void shouldRejectShortOrNullWords() {
            when(dailyPuzzleRepository.findById("puz-123")).thenReturn(Optional.of(samplePuzzle));

            WordSubmissionResponse nullRes = puzzleService.validateAndScoreWord(userId, "puz-123", null);
            WordSubmissionResponse shortRes = puzzleService.validateAndScoreWord(userId, "puz-123", "   ");
            WordSubmissionResponse threeCharRes = puzzleService.validateAndScoreWord(userId, "puz-123", "APE");

            assertThat(nullRes.valid()).isFalse();
            assertThat(nullRes.errorCode()).isEqualTo(ErrorTypeCode.TOO_SHORT);

            assertThat(shortRes.valid()).isFalse();
            assertThat(shortRes.errorCode()).isEqualTo(ErrorTypeCode.TOO_SHORT);

            assertThat(threeCharRes.valid()).isFalse();
            assertThat(threeCharRes.errorCode()).isEqualTo(ErrorTypeCode.TOO_SHORT);
        }

        @Test
        @DisplayName("Should reject word already found by user in active session")
        void shouldRejectAlreadyFoundWord() {
            when(dailyPuzzleRepository.findById("puz-123")).thenReturn(Optional.of(samplePuzzle));
            when(gameSessionRepository.existsByUserIdAndPuzzleIdAndFoundWordsContaining(userId, "puz-123", "CASA"))
                    .thenReturn(true);

            WordSubmissionResponse response = puzzleService.validateAndScoreWord(userId, "puz-123", "CASA");

            assertThat(response.valid()).isFalse();
            assertThat(response.errorCode()).isEqualTo(ErrorTypeCode.ALREADY_FOUND);
            assertThat(response.errorMessage()).isEqualTo("Hai già trovato questa parola.");
        }

        @Test
        @DisplayName("Should reject word missing the center letter")
        void shouldRejectWordMissingCenterLetter() {
            when(dailyPuzzleRepository.findById("puz-123")).thenReturn(Optional.of(samplePuzzle));
            when(gameSessionRepository.existsByUserIdAndPuzzleIdAndFoundWordsContaining(anyString(), anyString(),
                    anyString()))
                    .thenReturn(false);

            WordSubmissionResponse response = puzzleService.validateAndScoreWord(userId, "puz-123", "ROBO");

            assertThat(response.valid()).isFalse();
            assertThat(response.errorCode()).isEqualTo(ErrorTypeCode.MISSING_CENTER);
        }

        @Test
        @DisplayName("Should validate and score a valid 4-letter standard word")
        void shouldScoreValidFourLetterWord() {
            PuzzleWord pw = PuzzleWord.builder().isMielegramma(false).build();

            when(dailyPuzzleRepository.findById("puz-123")).thenReturn(Optional.of(samplePuzzle));
            when(gameSessionRepository.existsByUserIdAndPuzzleIdAndFoundWordsContaining(anyString(), anyString(),
                    anyString()))
                    .thenReturn(false);
            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord("puz-123", "CASA")).thenReturn(Optional.of(pw));
            when(scoringService.calculateWordScore("CASA", false)).thenReturn(1);

            WordSubmissionResponse response = puzzleService.validateAndScoreWord(userId, "puz-123", "casa");

            assertThat(response.valid()).isTrue();
            assertThat(response.word()).isEqualTo("CASA");
            assertThat(response.score()).isEqualTo(1);
            assertThat(response.isMielegramma()).isFalse();
        }

        @Test
        @DisplayName("Should validate and score a Mielegramma word using ScoringService")
        void shouldScoreValidMielegrammaWord() {
            PuzzleWord pw = PuzzleWord.builder().isMielegramma(true).build();

            when(dailyPuzzleRepository.findById("puz-123")).thenReturn(Optional.of(samplePuzzle));
            when(gameSessionRepository.existsByUserIdAndPuzzleIdAndFoundWordsContaining(anyString(), anyString(),
                    anyString()))
                    .thenReturn(false);
            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord("puz-123", "ALBERGO")).thenReturn(Optional.of(pw));
            when(scoringService.calculateWordScore("ALBERGO", true)).thenReturn(14);

            WordSubmissionResponse response = puzzleService.validateAndScoreWord(userId, "puz-123", "ALBERGO");

            assertThat(response.valid()).isTrue();
            assertThat(response.score()).isEqualTo(14);
            assertThat(response.isMielegramma()).isTrue();
        }

        @Test
        @DisplayName("Should reject word not present in dictionary")
        void shouldRejectWordNotInDictionary() {
            when(dailyPuzzleRepository.findById("puz-123")).thenReturn(Optional.of(samplePuzzle));
            when(gameSessionRepository.existsByUserIdAndPuzzleIdAndFoundWordsContaining(anyString(), anyString(),
                    anyString()))
                    .thenReturn(false);
            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord("puz-123", "AMARONE")).thenReturn(Optional.empty());

            WordSubmissionResponse response = puzzleService.validateAndScoreWord(userId, "puz-123", "AMARONE");

            assertThat(response.valid()).isFalse();
            assertThat(response.errorCode()).isEqualTo(ErrorTypeCode.NOT_IN_DICTIONARY);
        }
    }
}