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
    @DisplayName("generateDailyPuzzleJob - Invokes generator with current date")
    void shouldGenerateDailyPuzzleJob() {
        puzzleScheduler.generateDailyPuzzleJob();

        verify(puzzleGeneratorService, times(1))
                .generateAndSavePuzzleForDate(LocalDate.now());
    }

    @Test
    @DisplayName("onApplicationStart - Invokes generator on startup")
    void shouldGeneratePuzzleOnApplicationStart() {
        puzzleScheduler.onApplicationStart();

        verify(puzzleGeneratorService, times(1))
                .generateAndSavePuzzleForDate(LocalDate.now());
    }

    @Test
    @DisplayName("generateDailyPuzzleJob - Handles exceptions gracefully")
    void shouldHandleExceptionGracefully() {
        doThrow(new RuntimeException("Database error"))
                .when(puzzleGeneratorService).generateAndSavePuzzleForDate(any(LocalDate.class));

        // Must not throw an exception out of the scheduled method
        puzzleScheduler.generateDailyPuzzleJob();

        verify(puzzleGeneratorService, times(1))
                .generateAndSavePuzzleForDate(LocalDate.now());
    }
}