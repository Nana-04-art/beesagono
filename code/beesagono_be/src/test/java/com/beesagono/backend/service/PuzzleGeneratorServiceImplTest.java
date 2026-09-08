package com.beesagono.backend.service;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.DictionaryWord;
import com.beesagono.backend.entity.PuzzleOuterLetter;
import com.beesagono.backend.entity.id.PuzzleOuterLetterId;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        @DisplayName("Should return existing puzzle when already present for the given date")
        void shouldReturnExistingPuzzleWhenAlreadyExists() {
            DailyPuzzle existingPuzzle = createDailyPuzzle("puzzle-existing", testDate, "A", 100);

            when(dailyPuzzleRepository.existsByPuzzleDate(testDate)).thenReturn(true);
            when(dailyPuzzleRepository.findByPuzzleDate(testDate)).thenReturn(Optional.of(existingPuzzle));

            DailyPuzzle result = puzzleGeneratorService.generateAndSavePuzzleForDate(testDate);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("puzzle-existing");
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
        @DisplayName("Should generate and save puzzle successfully when candidate pangrams are present")
        void shouldGenerateAndSavePuzzleSuccessfullyWithCandidates() {
            DictionaryWord shortWord = createDictionaryWord("AERA", 3);
            DictionaryWord pangramWord = createDictionaryWord("ALBERGO", 7);
            DailyPuzzle savedPuzzle = createDailyPuzzle("puzzle-1", testDate, "A", 15);

            when(dailyPuzzleRepository.existsByPuzzleDate(testDate)).thenReturn(false);
            when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc()).thenReturn(Collections.emptyList());
            when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
            when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                    .thenReturn(List.of(shortWord, pangramWord));
            when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

            DailyPuzzle result = puzzleGeneratorService.generateAndSavePuzzleForDate(testDate);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("puzzle-1");

            verify(dailyPuzzleRepository, times(1)).save(any(DailyPuzzle.class));
            verify(puzzleOuterLetterRepository, times(1)).saveAll(any());
            verify(puzzleWordRepository, times(1)).saveAll(any());
        }

        @Test
        @DisplayName("Should generate and save puzzle successfully using fallback candidates when no pangrams found")
        void shouldGenerateAndSavePuzzleSuccessfullyWhenNoCandidatesFound() {
            DictionaryWord pangramWord = createDictionaryWord("ALBERGO", 7);
            DailyPuzzle savedPuzzle = createDailyPuzzle("puzzle-1", testDate, "A", 100);

            when(dailyPuzzleRepository.existsByPuzzleDate(testDate)).thenReturn(false);
            when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc()).thenReturn(Collections.emptyList());
            when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(Collections.emptyList());
            when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                    .thenReturn(List.of(pangramWord));
            when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

            DailyPuzzle result = puzzleGeneratorService.generateAndSavePuzzleForDate(testDate);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("puzzle-1");

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
            DictionaryWord pangramWord = createDictionaryWord("ALBERGO", 7);

            when(dailyPuzzleRepository.existsByPuzzleDate(testDate)).thenReturn(false);
            when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc()).thenReturn(Collections.emptyList());
            when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
            when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                    .thenReturn(List.of(pangramWord));
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
            DictionaryWord pangramWord = createDictionaryWord("ALBERGO", 7);
            DailyPuzzle savedPuzzle = createDailyPuzzle("puzzle-1", testDate, "A", 100);

            when(dailyPuzzleRepository.existsByPuzzleDate(testDate)).thenReturn(false);
            when(dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc()).thenReturn(Collections.emptyList());
            when(dictionaryWordRepository.findCandidatePangrams()).thenReturn(List.of("ALBERGO"));
            when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                    .thenReturn(List.of(pangramWord));
            when(dailyPuzzleRepository.save(any(DailyPuzzle.class))).thenReturn(savedPuzzle);

            when(puzzleOuterLetterRepository.saveAll(any()))
                    .thenThrow(new RuntimeException("Database error during outer letters save"));

            assertThatThrownBy(() -> puzzleGeneratorService.generateAndSavePuzzleForDate(testDate))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error during outer letters save");

            verify(puzzleWordRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("recalculatePuzzleWords - Word Recalculation")
    class RecalculateWordsTests {

        @Test
        @DisplayName("Should do nothing if puzzle or centerLetter is null")
        void shouldDoNothingWhenRecalculatingWithNullPuzzleOrCenterLetter() {
            puzzleGeneratorService.recalculatePuzzleWords(null);

            DailyPuzzle invalidPuzzle = createDailyPuzzle("puzzle-invalid", testDate, null, 0);
            puzzleGeneratorService.recalculatePuzzleWords(invalidPuzzle);

            verify(dictionaryWordRepository, never()).findValidWordsForPuzzle(anyInt(), anyInt());
            verify(puzzleWordRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should clear existing words and recalculate max score")
        void shouldRecalculatePuzzleWordsSuccessfully() {
            String puzzleId = "puzzle-1";
            DailyPuzzle puzzle = createDailyPuzzle(puzzleId, testDate, "A", 0);

            PuzzleOuterLetter outerLetterB = createPuzzleOuterLetter(puzzleId, "B");
            PuzzleOuterLetter outerLetterR = createPuzzleOuterLetter(puzzleId, "R");
            puzzle.getOuterLetters().addAll(List.of(outerLetterB, outerLetterR));

            DictionaryWord dictWord1 = createDictionaryWord("ARABA", 3);
            DictionaryWord dictWord2 = createDictionaryWord("RABA", 3);

            when(dictionaryWordRepository.findValidWordsForPuzzle(anyInt(), anyInt()))
                    .thenReturn(List.of(dictWord1, dictWord2));
            when(scoringService.calculateWordScore("ARABA", false)).thenReturn(5);
            when(scoringService.calculateWordScore("RABA", false)).thenReturn(1);

            puzzleGeneratorService.recalculatePuzzleWords(puzzle);

            assertThat(puzzle.getMaxScore()).isEqualTo(6);
            verify(puzzleWordRepository, times(1)).saveAll(any());
        }
    }

    // --- Helper Methods ---

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