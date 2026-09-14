package com.beesagono.backend.scheduler;

import com.beesagono.backend.service.PuzzleGeneratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PuzzleSchedulerTest {

    @Mock
    private PuzzleGeneratorService puzzleGeneratorService;

    @InjectMocks
    private PuzzleScheduler puzzleScheduler;

    @Test
    @DisplayName("generateDailyPuzzleJob - Invokes generator with tomorrow's date")
    void shouldGenerateDailyPuzzleJob() {
        puzzleScheduler.generateDailyPuzzleJob();

        verify(puzzleGeneratorService, times(1))
                .generateAndSavePuzzleForDate(LocalDate.now().plusDays(1));
    }

    @Test
    @DisplayName("onApplicationStart - Invokes generator for today and tomorrow on startup")
    void shouldGeneratePuzzleOnApplicationStart() {
        puzzleScheduler.onApplicationStart();

        verify(puzzleGeneratorService, times(1))
                .generateAndSavePuzzleForDate(LocalDate.now());
        verify(puzzleGeneratorService, times(1))
                .generateAndSavePuzzleForDate(LocalDate.now().plusDays(1));
    }

    @Test
    @DisplayName("generateDailyPuzzleJob - Handles exceptions gracefully")
    void shouldHandleExceptionGracefully() {
        doThrow(new RuntimeException("Database error"))
                .when(puzzleGeneratorService).generateAndSavePuzzleForDate(any(LocalDate.class));

        puzzleScheduler.generateDailyPuzzleJob();

        verify(puzzleGeneratorService, times(1))
                .generateAndSavePuzzleForDate(LocalDate.now().plusDays(1));
    }
}