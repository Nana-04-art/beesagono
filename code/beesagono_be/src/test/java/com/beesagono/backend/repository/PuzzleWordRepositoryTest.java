package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.DictionaryWord;
import com.beesagono.backend.entity.PuzzleWord;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.PuzzleWordId;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class PuzzleWordRepositoryTest {

    @Autowired
    private PuzzleWordRepository puzzleWordRepository;

    @Autowired
    private TestEntityManager entityManager;

    private DailyPuzzle puzzle;
    private DictionaryWord dictWord;

    @BeforeEach
    void setUp() {
        User user = entityManager.persist(User.builder()
                .username("usr")
                .email("u@e.com")
                .passwordHash("pwd")
                .build());

        puzzle = entityManager.persist(DailyPuzzle.builder()
                .puzzleDate(LocalDate.now())
                .centerLetter("A")
                .maxScore(50)
                .seed("seed")
                .build());

        dictWord = entityManager.persist(DictionaryWord.builder()
                .word("ALBERGO")
                .wordLength(7)
                .uniqueLettersCount(7)
                .letterMask(182355)
                .isCandidatePangram(true)
                .addedByUser(user)
                .addedAt(new Date())
                .build());
    }

    // --- findByIdPuzzleIdAndIdWord ---

    @Test
    @DisplayName("findByIdPuzzleIdAndIdWord - Should return PuzzleWord when composite key matches")
    void shouldFindByIdPuzzleIdAndIdWord() {
        PuzzleWord pw = PuzzleWord.builder()
                .id(new PuzzleWordId(puzzle.getId(), "ALBERGO"))
                .puzzle(puzzle)
                .dictionaryWord(dictWord)
                .isMielegramma(true)
                .build();
        entityManager.persistAndFlush(pw);

        Optional<PuzzleWord> result = puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzle.getId(), "ALBERGO");

        assertThat(result).isPresent();
        assertThat(result.get().getDictionaryWord().getWord()).isEqualTo("ALBERGO");
    }

    @Test
    @DisplayName("findByIdPuzzleIdAndIdWord - Should return empty Optional when not found")
    void shouldReturnEmptyWhenPuzzleWordNotFound() {
        Optional<PuzzleWord> result = puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzle.getId(), "INEXISTENT");

        assertThat(result).isEmpty();
    }

    // --- countByIdPuzzleId ---

    @Test
    @DisplayName("countByIdPuzzleId - Should return correct word count for puzzle")
    void shouldCountByIdPuzzleId() {
        PuzzleWord pw = PuzzleWord.builder()
                .id(new PuzzleWordId(puzzle.getId(), "ALBERGO"))
                .puzzle(puzzle)
                .dictionaryWord(dictWord)
                .isMielegramma(true)
                .build();
        entityManager.persistAndFlush(pw);

        int count = puzzleWordRepository.countByIdPuzzleId(puzzle.getId());

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("countByIdPuzzleId - Should return zero when puzzle has no words")
    void shouldReturnZeroWhenPuzzleHasNoWords() {
        int count = puzzleWordRepository.countByIdPuzzleId("fake-puzzle-id");

        assertThat(count).isZero();
    }
}