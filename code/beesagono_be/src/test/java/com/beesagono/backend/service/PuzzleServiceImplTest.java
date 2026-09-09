package com.beesagono.backend.service;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.dto.puzzle.WordSubmissionResponse;
import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.PuzzleWord;
import com.beesagono.backend.enums.ErrorTypeCode;
import com.beesagono.backend.mapper.DailyPuzzleMapper;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import com.beesagono.backend.repository.PuzzleWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PuzzleServiceImplTest {

    @Mock
    private DailyPuzzleRepository dailyPuzzleRepository;

    @Mock
    private PuzzleWordRepository puzzleWordRepository;

    @Mock
    private PuzzleGeneratorService puzzleGeneratorService;

    @Mock
    private DailyPuzzleMapper dailyPuzzleMapper;

    @InjectMocks
    private PuzzleServiceImpl puzzleService;

    private DailyPuzzle samplePuzzle;
    private DailyPuzzleResponse sampleResponse;

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

    @Test
    @DisplayName("getTodayPuzzle - Generates puzzle if missing and returns mapped DTO")
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
    @DisplayName("getTodayPuzzle - Uses existing puzzle without triggering generation")
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
    @DisplayName("getTodayPuzzle - Throws exception when puzzle cannot be found after generation attempt")
    void shouldThrowExceptionWhenPuzzleNotFound() {
        LocalDate today = LocalDate.now();
        when(dailyPuzzleRepository.existsByPuzzleDate(today)).thenReturn(false);
        when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> puzzleService.getTodayPuzzle())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Puzzle del giorno non trovato");
    }

    // --- validateAndScoreWord ---

    @Test
    @DisplayName("validateAndScoreWord - Reject null, blank or short words (<4 chars)")
    void shouldRejectShortOrNullWords() {
        WordSubmissionResponse nullRes = puzzleService.validateAndScoreWord("puz-123", null);
        WordSubmissionResponse shortRes = puzzleService.validateAndScoreWord("puz-123", "   ");
        WordSubmissionResponse threeCharRes = puzzleService.validateAndScoreWord("puz-123", "APE");

        assertThat(nullRes.valid()).isFalse();
        assertThat(nullRes.errorCode()).isEqualTo(ErrorTypeCode.TOO_SHORT);

        assertThat(shortRes.valid()).isFalse();
        assertThat(threeCharRes.valid()).isFalse();
    }

    @Test
    @DisplayName("validateAndScoreWord - Reject word missing center letter")
    void shouldRejectWordMissingCenterLetter() {
        when(dailyPuzzleRepository.findById("puz-123")).thenReturn(Optional.of(samplePuzzle));

        WordSubmissionResponse response = puzzleService.validateAndScoreWord("puz-123", "ROBO");

        assertThat(response.valid()).isFalse();
        assertThat(response.errorCode()).isEqualTo(ErrorTypeCode.MISSING_CENTER);
    }

    @Test
    @DisplayName("validateAndScoreWord - Score valid 4-letter standard word")
    void shouldScoreValidFourLetterWord() {
        PuzzleWord pw = PuzzleWord.builder().isMielegramma(false).build();

        when(dailyPuzzleRepository.findById("puz-123")).thenReturn(Optional.of(samplePuzzle));
        when(puzzleWordRepository.findByIdPuzzleIdAndIdWord("puz-123", "CASA")).thenReturn(Optional.of(pw));

        WordSubmissionResponse response = puzzleService.validateAndScoreWord("puz-123", "casa");

        assertThat(response.valid()).isTrue();
        assertThat(response.word()).isEqualTo("CASA");
        assertThat(response.score()).isEqualTo(1);
        assertThat(response.isMielegramma()).isFalse();
    }

    @Test
    @DisplayName("validateAndScoreWord - Score valid word longer than 4 letters with Mielegramma bonus")
    void shouldScoreValidMielegrammaWord() {
        PuzzleWord pw = PuzzleWord.builder().isMielegramma(true).build();

        when(dailyPuzzleRepository.findById("puz-123")).thenReturn(Optional.of(samplePuzzle));
        when(puzzleWordRepository.findByIdPuzzleIdAndIdWord("puz-123", "ALBERGO")).thenReturn(Optional.of(pw));

        WordSubmissionResponse response = puzzleService.validateAndScoreWord("puz-123", "ALBERGO");

        assertThat(response.valid()).isTrue();
        assertThat(response.score()).isEqualTo(14);
        assertThat(response.isMielegramma()).isTrue();
    }

    @Test
    @DisplayName("validateAndScoreWord - Reject word not present in dictionary")
    void shouldRejectWordNotInDictionary() {
        when(dailyPuzzleRepository.findById("puz-123")).thenReturn(Optional.of(samplePuzzle));
        when(puzzleWordRepository.findByIdPuzzleIdAndIdWord("puz-123", "AMARONE")).thenReturn(Optional.empty());

        WordSubmissionResponse response = puzzleService.validateAndScoreWord("puz-123", "AMARONE");

        assertThat(response.valid()).isFalse();
        assertThat(response.errorCode()).isEqualTo(ErrorTypeCode.NOT_IN_DICTIONARY);
    }
}