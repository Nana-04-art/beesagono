package com.beesagono.backend.service;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.DictionaryWord;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import com.beesagono.backend.repository.DictionaryWordRepository;
import com.beesagono.backend.repository.PuzzleOuterLetterRepository;
import com.beesagono.backend.repository.PuzzleWordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
<<<<<<< HEAD
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
=======

import java.time.LocalDate;
import java.util.List;

>>>>>>> d5e5c21 (code/beesagono_be: update service test)
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

    @InjectMocks
    private PuzzleGeneratorServiceImpl puzzleGeneratorService;

    @Test
<<<<<<< HEAD
    @DisplayName("generateAndSavePuzzleForDate - Idempotency check: does nothing when puzzle already exists")
=======
    @DisplayName("generateAndSavePuzzleForDate - Does nothing when puzzle already exists")
>>>>>>> d5e5c21 (code/beesagono_be: update service test)
    void shouldDoNothingWhenPuzzleAlreadyExists() {
        LocalDate date = LocalDate.of(2026, 9, 8);

        when(dailyPuzzleRepository.existsByPuzzleDate(date)).thenReturn(true);

        puzzleGeneratorService.generateAndSavePuzzleForDate(date);

        verify(dailyPuzzleRepository, never()).save(any());
        verify(puzzleOuterLetterRepository, never()).saveAll(any());
        verify(puzzleWordRepository, never()).saveAll(any());
    }

    @Test
<<<<<<< HEAD
    @DisplayName("generateAndSavePuzzleForDate - Generates and saves puzzle successfully when Quality Gate is passed")
    void shouldGenerateAndSavePuzzleSuccessfully() {
        LocalDate date = LocalDate.of(2026, 9, 8);

        List<DictionaryWord> validWords = new ArrayList<>();
        validWords.add(DictionaryWord.builder()
                .word("ALBERGO")
                .uniqueLettersCount(7)
                .build()); // Mielegramma

        for (int i = 0; i < 20; i++) {
            validWords.add(DictionaryWord.builder()
                    .word("ALBA" + i)
                    .uniqueLettersCount(4)
                    .build());
        }
=======
    @DisplayName("generateAndSavePuzzleForDate - Generates and saves puzzle successfully")
    void shouldGenerateAndSavePuzzleSuccessfully() {
        LocalDate date = LocalDate.of(2026, 9, 8);

        DictionaryWord pangramWord = DictionaryWord.builder()
                .word("ALBERGO")
                .uniqueLettersCount(7)
                .build();
>>>>>>> d5e5c21 (code/beesagono_be: update service test)

        DailyPuzzle savedPuzzle = DailyPuzzle.builder()
                .id("puzzle-1")
                .puzzleDate(date)
                .centerLetter("A")
                .maxScore(100)
                .build();

        when(dailyPuzzleRepository.existsByPuzzleDate(date)).thenReturn(false);
<<<<<<< HEAD
        when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc(any(Pageable.class)))
                .thenReturn(List.of());
        when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
        when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                .thenReturn(validWords);
=======
        when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
        when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                .thenReturn(List.of(pangramWord));
>>>>>>> d5e5c21 (code/beesagono_be: update service test)
        when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

        puzzleGeneratorService.generateAndSavePuzzleForDate(date);

        verify(dailyPuzzleRepository, times(1)).save(any(DailyPuzzle.class));
        verify(puzzleOuterLetterRepository, times(1)).saveAll(any());
        verify(puzzleWordRepository, times(1)).saveAll(any());
    }
<<<<<<< HEAD

    @Test
    @DisplayName("generateAndSavePuzzleForDate - Fallback mode: uses best available board when Quality Gate fails after max attempts")
    void shouldFallbackToBestBoardWhenQualityGateFails() {
        LocalDate date = LocalDate.of(2026, 9, 8);

        // We generate a single word per attempt, so the Quality Gate is never
        // passed (requires >= 20 words)
        List<DictionaryWord> validWords = List.of(
                DictionaryWord.builder()
                        .word("ALBERGO")
                        .uniqueLettersCount(7)
                        .build());

        DailyPuzzle savedPuzzle = DailyPuzzle.builder()
                .id("puzzle-fallback-1")
                .puzzleDate(date)
                .centerLetter("A")
                .maxScore(14)
                .build();

        when(dailyPuzzleRepository.existsByPuzzleDate(date)).thenReturn(false);
        when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc(any(Pageable.class)))
                .thenReturn(List.of());
        when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
        when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                .thenReturn(validWords);
        when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

        puzzleGeneratorService.generateAndSavePuzzleForDate(date);

        // Ensure that the best board is saved anyway after 50 unsuccessful attempts
        // (fallback)
        verify(dailyPuzzleRepository, times(1)).save(any(DailyPuzzle.class));
        verify(puzzleOuterLetterRepository, times(1)).saveAll(any());
        verify(puzzleWordRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("generateAndSavePuzzleForDate - Propagates exception when dailyPuzzleRepository.save fails")
    void shouldPropagateExceptionWhenPuzzleSaveFails() {
        LocalDate date = LocalDate.of(2026, 9, 8);

        List<DictionaryWord> validWords = List.of(
                DictionaryWord.builder().word("ALBERGO").uniqueLettersCount(7).build());

        when(dailyPuzzleRepository.existsByPuzzleDate(date)).thenReturn(false);
        when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc(any(Pageable.class))).thenReturn(List.of());
        when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
        when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt())).thenReturn(validWords);

        when(dailyPuzzleRepository.save(any(DailyPuzzle.class)))
                .thenThrow(new RuntimeException("Database error during puzzle save"));

        assertThatThrownBy(() -> puzzleGeneratorService.generateAndSavePuzzleForDate(date))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error during puzzle save");

        verify(puzzleOuterLetterRepository, never()).saveAll(any());
        verify(puzzleWordRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("generateAndSavePuzzleForDate - Propagates exception when outer letters save fails")
    void shouldPropagateExceptionWhenOuterLettersSaveFails() {
        LocalDate date = LocalDate.of(2026, 9, 8);

        List<DictionaryWord> validWords = List.of(
                DictionaryWord.builder().word("ALBERGO").uniqueLettersCount(7).build());

        DailyPuzzle savedPuzzle = DailyPuzzle.builder()
                .id("puzzle-1")
                .puzzleDate(date)
                .centerLetter("A")
                .maxScore(100)
                .build();

        when(dailyPuzzleRepository.existsByPuzzleDate(date)).thenReturn(false);
        when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc(any(Pageable.class))).thenReturn(List.of());
        when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
        when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt())).thenReturn(validWords);
        when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

        when(puzzleOuterLetterRepository.saveAll(any()))
                .thenThrow(new RuntimeException("Database error during outer letters save"));

        assertThatThrownBy(() -> puzzleGeneratorService.generateAndSavePuzzleForDate(date))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error during outer letters save");

        verify(puzzleWordRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("generateAndSavePuzzleForDate - Propagates exception when puzzle words save fails")
    void shouldPropagateExceptionWhenPuzzleWordsSaveFails() {
        LocalDate date = LocalDate.of(2026, 9, 8);

        List<DictionaryWord> validWords = List.of(
                DictionaryWord.builder().word("ALBERGO").uniqueLettersCount(7).build());

        DailyPuzzle savedPuzzle = DailyPuzzle.builder()
                .id("puzzle-1")
                .puzzleDate(date)
                .centerLetter("A")
                .maxScore(100)
                .build();

        when(dailyPuzzleRepository.existsByPuzzleDate(date)).thenReturn(false);
        when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc(any(Pageable.class))).thenReturn(List.of());
        when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
        when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt())).thenReturn(validWords);
        when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

        when(puzzleWordRepository.saveAll(any()))
                .thenThrow(new RuntimeException("Database error during puzzle words save"));

        assertThatThrownBy(() -> puzzleGeneratorService.generateAndSavePuzzleForDate(date))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error during puzzle words save");

        verify(dailyPuzzleRepository, times(1)).save(any(DailyPuzzle.class));
        verify(puzzleOuterLetterRepository, times(1)).saveAll(any());
    }
=======
>>>>>>> d5e5c21 (code/beesagono_be: update service test)
}