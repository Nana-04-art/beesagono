package com.beesagono.backend.service;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.DictionaryWord;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import com.beesagono.backend.repository.DictionaryWordRepository;
import com.beesagono.backend.repository.PuzzleOuterLetterRepository;
import com.beesagono.backend.repository.PuzzleWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PuzzleGeneratorServiceImplTest {

    @Mock
    private DictionaryWordRepository dictionaryWordRepository;

    @Mock
    private DailyPuzzleRepository dailyPuzzleRepository;

    @Mock
    private PuzzleOuterLetterRepository puzzleOuterLetterRepository;

    @Mock
    private PuzzleWordRepository puzzleWordRepository;

    @Mock
    private ScoringService scoringService;

    @InjectMocks
    private PuzzleGeneratorServiceImpl puzzleGeneratorService;

    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 9, 8);
    }

    @Nested
    @DisplayName("generateAndSavePuzzleForDate - Idempotency Checks")
    class IdempotencyTests {

        @Test
        @DisplayName("Should do nothing when a puzzle already exists for the given date")
        void shouldDoNothingWhenPuzzleAlreadyExists() {
            when(dailyPuzzleRepository.existsByPuzzleDate(testDate)).thenReturn(true);

            puzzleGeneratorService.generateAndSavePuzzleForDate(testDate);

            verify(dailyPuzzleRepository, never()).save(any());
            verify(puzzleOuterLetterRepository, never()).saveAll(any());
            verify(puzzleWordRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("generateAndSavePuzzleForDate - Puzzle Generation Flow")
    class GenerationFlowTests {

        @BeforeEach
        void setUpScoringMock() {
            when(scoringService.calculateWordScore(anyString(), anyBoolean())).thenReturn(4);
        }

        @Test
        @DisplayName("Should generate and save puzzle successfully when Quality Gate is passed")
        void shouldGenerateAndSavePuzzleSuccessfully() {
            List<DictionaryWord> validWords = new ArrayList<>();
            validWords.add(DictionaryWord.builder()
                    .word("ALBERGO")
                    .uniqueLettersCount(7)
                    .build());

            for (int i = 0; i < 20; i++) {
                validWords.add(DictionaryWord.builder()
                        .word("ALBA" + i)
                        .uniqueLettersCount(4)
                        .build());
            }

            DailyPuzzle savedPuzzle = DailyPuzzle.builder()
                    .id("puzzle-1")
                    .puzzleDate(testDate)
                    .centerLetter("A")
                    .maxScore(100)
                    .build();

            when(dailyPuzzleRepository.existsByPuzzleDate(testDate)).thenReturn(false);
            when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc(any(Pageable.class)))
                    .thenReturn(List.of());
            when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
            when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                    .thenReturn(validWords);
            when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

            puzzleGeneratorService.generateAndSavePuzzleForDate(testDate);

            verify(dailyPuzzleRepository, times(1)).save(any(DailyPuzzle.class));
            verify(puzzleOuterLetterRepository, times(1)).saveAll(any());
            verify(puzzleWordRepository, times(1)).saveAll(any());
        }

        @Test
        @DisplayName("Should fallback to best available board when Quality Gate fails after max attempts")
        void shouldFallbackToBestBoardWhenQualityGateFails() {
            List<DictionaryWord> validWords = List.of(
                    DictionaryWord.builder()
                            .word("ALBERGO")
                            .uniqueLettersCount(7)
                            .build());

            DailyPuzzle savedPuzzle = DailyPuzzle.builder()
                    .id("puzzle-fallback-1")
                    .puzzleDate(testDate)
                    .centerLetter("A")
                    .maxScore(14)
                    .build();

            when(dailyPuzzleRepository.existsByPuzzleDate(testDate)).thenReturn(false);
            when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc(any(Pageable.class)))
                    .thenReturn(List.of());
            when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
            when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                    .thenReturn(validWords);
            when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

            puzzleGeneratorService.generateAndSavePuzzleForDate(testDate);

            verify(dailyPuzzleRepository, times(1)).save(any(DailyPuzzle.class));
            verify(puzzleOuterLetterRepository, times(1)).saveAll(any());
            verify(puzzleWordRepository, times(1)).saveAll(any());
        }
    }

    @Nested
    @DisplayName("generateAndSavePuzzleForDate - Error Handling & Exception Propagation")
    class ExceptionHandlingTests {

        @BeforeEach
        void setUpScoringMock() {
            when(scoringService.calculateWordScore(anyString(), anyBoolean())).thenReturn(4);
        }

        @Test
        @DisplayName("Should propagate exception when dailyPuzzleRepository.save fails")
        void shouldPropagateExceptionWhenPuzzleSaveFails() {
            List<DictionaryWord> validWords = List.of(
                    DictionaryWord.builder().word("ALBERGO").uniqueLettersCount(7).build());

            when(dailyPuzzleRepository.existsByPuzzleDate(testDate)).thenReturn(false);
            when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc(any(Pageable.class))).thenReturn(List.of());
            when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
            when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt())).thenReturn(validWords);

            when(dailyPuzzleRepository.save(any(DailyPuzzle.class)))
                    .thenThrow(new RuntimeException("Database error during puzzle save"));

            assertThatThrownBy(() -> puzzleGeneratorService.generateAndSavePuzzleForDate(testDate))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error during puzzle save");

            verify(puzzleOuterLetterRepository, never()).saveAll(any());
            verify(puzzleWordRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should propagate exception when outer letters save fails")
        void shouldPropagateExceptionWhenOuterLettersSaveFails() {
            List<DictionaryWord> validWords = List.of(
                    DictionaryWord.builder().word("ALBERGO").uniqueLettersCount(7).build());

            DailyPuzzle savedPuzzle = DailyPuzzle.builder()
                    .id("puzzle-1")
                    .puzzleDate(testDate)
                    .centerLetter("A")
                    .maxScore(100)
                    .build();

            when(dailyPuzzleRepository.existsByPuzzleDate(testDate)).thenReturn(false);
            when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc(any(Pageable.class))).thenReturn(List.of());
            when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
            when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt())).thenReturn(validWords);
            when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

            when(puzzleOuterLetterRepository.saveAll(any()))
                    .thenThrow(new RuntimeException("Database error during outer letters save"));

            assertThatThrownBy(() -> puzzleGeneratorService.generateAndSavePuzzleForDate(testDate))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error during outer letters save");

            verify(puzzleWordRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should propagate exception when puzzle words save fails")
        void shouldPropagateExceptionWhenPuzzleWordsSaveFails() {
            List<DictionaryWord> validWords = List.of(
                    DictionaryWord.builder().word("ALBERGO").uniqueLettersCount(7).build());

            DailyPuzzle savedPuzzle = DailyPuzzle.builder()
                    .id("puzzle-1")
                    .puzzleDate(testDate)
                    .centerLetter("A")
                    .maxScore(100)
                    .build();

            when(dailyPuzzleRepository.existsByPuzzleDate(testDate)).thenReturn(false);
            when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc(any(Pageable.class))).thenReturn(List.of());
            when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
            when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt())).thenReturn(validWords);
            when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

            when(puzzleWordRepository.saveAll(any()))
                    .thenThrow(new RuntimeException("Database error during puzzle words save"));

            assertThatThrownBy(() -> puzzleGeneratorService.generateAndSavePuzzleForDate(testDate))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error during puzzle words save");

        verify(dailyPuzzleRepository, times(1)).save(any(DailyPuzzle.class));
            verify(puzzleOuterLetterRepository, times(1)).saveAll(any());
        }
    }
}