package com.beesagono.backend.service;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.DictionaryWord;
import com.beesagono.backend.entity.PuzzleOuterLetter;
import com.beesagono.backend.entity.id.PuzzleOuterLetterId;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("generateAndSavePuzzleForDate - Returns existing puzzle when already present")
    void shouldReturnExistingPuzzleWhenAlreadyExists() {
        LocalDate date = LocalDate.of(2026, 9, 8);
        DailyPuzzle existingPuzzle = createDailyPuzzle("puzzle-existing", date, "A", 100);

        when(dailyPuzzleRepository.existsByPuzzleDate(date)).thenReturn(true);
        when(dailyPuzzleRepository.findByPuzzleDate(date)).thenReturn(Optional.of(existingPuzzle));

        DailyPuzzle result = puzzleGeneratorService.generateAndSavePuzzleForDate(date);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("puzzle-existing");
        verify(dailyPuzzleRepository, never()).save(any());
        verify(puzzleOuterLetterRepository, never()).saveAll(any());
        verify(puzzleWordRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("generateAndSavePuzzleForDate - Generates and saves puzzle successfully using fallback candidates")
    void shouldGenerateAndSavePuzzleSuccessfullyWhenNoCandidatesFound() {
        LocalDate date = LocalDate.of(2026, 9, 8);
        DictionaryWord pangramWord = createDictionaryWord("ALBERGO", 7);
        DailyPuzzle savedPuzzle = createDailyPuzzle("puzzle-1", date, "A", 100);

        when(dailyPuzzleRepository.existsByPuzzleDate(date)).thenReturn(false);
        when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc()).thenReturn(Collections.emptyList());
        when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(Collections.emptyList());
        when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                .thenReturn(List.of(pangramWord));
        when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

        DailyPuzzle result = puzzleGeneratorService.generateAndSavePuzzleForDate(date);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("puzzle-1");

        verify(dailyPuzzleRepository, times(1)).save(any(DailyPuzzle.class));
        verify(puzzleOuterLetterRepository, times(2)).saveAll(any());
        verify(puzzleWordRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("generateAndSavePuzzleForDate - Generates and saves puzzle successfully with candidates")
    void shouldGenerateAndSavePuzzleSuccessfully() {
        LocalDate date = LocalDate.of(2026, 9, 8);
        DictionaryWord shortWord = createDictionaryWord("AERA", 3);
        DictionaryWord pangramWord = createDictionaryWord("ALBERGO", 7);
        DailyPuzzle savedPuzzle = createDailyPuzzle("puzzle-1", date, "A", 15);

        when(dailyPuzzleRepository.existsByPuzzleDate(date)).thenReturn(false);
        when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc()).thenReturn(Collections.emptyList());
        when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
        when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                .thenReturn(List.of(shortWord, pangramWord));
        when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

        DailyPuzzle result = puzzleGeneratorService.generateAndSavePuzzleForDate(date);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("puzzle-1");

        verify(dailyPuzzleRepository, times(1)).save(any(DailyPuzzle.class));
        verify(puzzleOuterLetterRepository, times(2)).saveAll(any());
        verify(puzzleWordRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("recalculatePuzzleWords - Does nothing if puzzle or centerLetter is null")
    void shouldDoNothingWhenRecalculatingWithNullPuzzleOrCenterLetter() {
        puzzleGeneratorService.recalculatePuzzleWords(null);

        DailyPuzzle invalidPuzzle = createDailyPuzzle("puzzle-invalid", LocalDate.now(), null, 0);
        puzzleGeneratorService.recalculatePuzzleWords(invalidPuzzle);

        verify(dictionaryWordRepository, never()).findValidWordsForPuzzle(anyInt(), anyInt());
        verify(puzzleWordRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("recalculatePuzzleWords - Clears existing words and recalculates max score")
    void shouldRecalculatePuzzleWordsSuccessfully() {
        String puzzleId = "puzzle-1";
        DailyPuzzle puzzle = createDailyPuzzle(puzzleId, LocalDate.now(), "A", 0);

        PuzzleOuterLetter outerLetterB = createPuzzleOuterLetter(puzzleId, "B");
        PuzzleOuterLetter outerLetterR = createPuzzleOuterLetter(puzzleId, "R");
        puzzle.getOuterLetters().addAll(List.of(outerLetterB, outerLetterR));

        DictionaryWord dictWord1 = createDictionaryWord("ARABA", 3); // 5 letters -> 5 points
        DictionaryWord dictWord2 = createDictionaryWord("RABA", 3); // 4 letters -> 1 point

        when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                .thenReturn(List.of(dictWord1, dictWord2));

        puzzleGeneratorService.recalculatePuzzleWords(puzzle);

        assertThat(puzzle.getMaxScore()).isEqualTo(6);
        verify(puzzleWordRepository, times(1)).saveAll(any());
    }

    // --- Private Helper Methods ---

    private DailyPuzzle createDailyPuzzle(String id, LocalDate date, String centerLetter, int maxScore) {
        return DailyPuzzle.builder()
                .id(id)
                .puzzleDate(date)
                .centerLetter(centerLetter)
                .maxScore(maxScore)
                .outerLetters(new ArrayList<>())
                .puzzleWords(new ArrayList<>())
                .build();
    }

    private DictionaryWord createDictionaryWord(String word, int uniqueLetters) {
        return DictionaryWord.builder()
                .word(word)
                .wordLength(word.length())
                .uniqueLettersCount(uniqueLetters)
                .build();
    }

    private PuzzleOuterLetter createPuzzleOuterLetter(String puzzleId, String letter) {
        return PuzzleOuterLetter.builder()
                .id(new PuzzleOuterLetterId(puzzleId, letter))
                .build();
    }
}