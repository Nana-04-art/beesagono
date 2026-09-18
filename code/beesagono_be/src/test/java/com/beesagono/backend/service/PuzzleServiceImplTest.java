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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    private DailyPuzzle mockPuzzle;
    private DailyPuzzleResponse mockResponse;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();
        mockPuzzle = DailyPuzzle.builder()
                .id("puzzle-today")
                .puzzleDate(today)
                .centerLetter("A")
                .build();

        mockResponse = DailyPuzzleResponse.builder()
                .id("puzzle-today")
                .puzzleDate(today)
                .centerLetter("A")
                .build();
    }

    // --- getTodayPuzzle ---

    @Test
    @DisplayName("getTodayPuzzle - Success when puzzle already exists")
    void shouldReturnTodayPuzzleWhenAlreadyExists() {
        LocalDate today = LocalDate.now();

        when(dailyPuzzleRepository.existsByPuzzleDate(today)).thenReturn(true);
        when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.of(mockPuzzle));
        when(dailyPuzzleMapper.toDailyPuzzleResponse(mockPuzzle)).thenReturn(mockResponse);

        DailyPuzzleResponse response = puzzleService.getTodayPuzzle();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("puzzle-today");

        verify(puzzleGeneratorService, never()).generateAndSavePuzzleForDate(any());
        verify(dailyPuzzleRepository, times(1)).findByPuzzleDate(today);
    }

    @Test
    @DisplayName("getTodayPuzzle - Generates puzzle if not existing then returns it")
    void shouldGeneratePuzzleIfNotExistingAndReturnIt() {
        LocalDate today = LocalDate.now();

        when(dailyPuzzleRepository.existsByPuzzleDate(today)).thenReturn(false);
        when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.of(mockPuzzle));
        when(dailyPuzzleMapper.toDailyPuzzleResponse(mockPuzzle)).thenReturn(mockResponse);

        DailyPuzzleResponse response = puzzleService.getTodayPuzzle();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("puzzle-today");

        verify(puzzleGeneratorService, times(1)).generateAndSavePuzzleForDate(today);
        verify(dailyPuzzleRepository, times(1)).findByPuzzleDate(today);
    }

    @Test
    @DisplayName("getTodayPuzzle - Throws ResponseStatusException when puzzle not found")
    void shouldThrowExceptionWhenPuzzleNotFound() {
        LocalDate today = LocalDate.now();

        when(dailyPuzzleRepository.existsByPuzzleDate(today)).thenReturn(false);
        when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> puzzleService.getTodayPuzzle())
                .isInstanceOf(ResponseStatusException.class)
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getReason()).isEqualTo("Puzzle del giorno non trovato");
                });
    }
}