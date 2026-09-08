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

import java.time.LocalDate;
import java.util.List;

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
    @DisplayName("generateAndSavePuzzleForDate - Does nothing when puzzle already exists")
    void shouldDoNothingWhenPuzzleAlreadyExists() {
        LocalDate date = LocalDate.of(2026, 9, 8);

        when(dailyPuzzleRepository.existsByPuzzleDate(date)).thenReturn(true);

        puzzleGeneratorService.generateAndSavePuzzleForDate(date);

        verify(dailyPuzzleRepository, never()).save(any());
        verify(puzzleOuterLetterRepository, never()).saveAll(any());
        verify(puzzleWordRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("generateAndSavePuzzleForDate - Generates and saves puzzle successfully")
    void shouldGenerateAndSavePuzzleSuccessfully() {
        LocalDate date = LocalDate.of(2026, 9, 8);

        DictionaryWord pangramWord = DictionaryWord.builder()
                .word("ALBERGO")
                .uniqueLettersCount(7)
                .build();

        DailyPuzzle savedPuzzle = DailyPuzzle.builder()
                .id("puzzle-1")
                .puzzleDate(date)
                .centerLetter("A")
                .maxScore(100)
                .build();

        when(dailyPuzzleRepository.existsByPuzzleDate(date)).thenReturn(false);
        when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
        when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                .thenReturn(List.of(pangramWord));
        when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

        puzzleGeneratorService.generateAndSavePuzzleForDate(date);

        verify(dailyPuzzleRepository, times(1)).save(any(DailyPuzzle.class));
        verify(puzzleOuterLetterRepository, times(1)).saveAll(any());
        verify(puzzleWordRepository, times(1)).saveAll(any());
    }
}