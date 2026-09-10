package com.beesagono.backend.service;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.mapper.DailyPuzzleMapper;
import com.beesagono.backend.repository.DailyPuzzleRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PuzzleServiceImplTest {

    @Mock
    private DailyPuzzleRepository dailyPuzzleRepository;

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
}