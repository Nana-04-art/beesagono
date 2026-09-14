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

    private DailyPuzzle defaultPuzzle;
    private DictionaryWord defaultDictWord;

    @BeforeEach
    void setUp() {
        User user = entityManager.persist(createUser("usr", "u@e.com"));
        defaultPuzzle = entityManager.persist(createDailyPuzzle(LocalDate.now(), "A", 50, "seed"));
        defaultDictWord = entityManager.persist(createDictionaryWord("ALBERGO", user));
    }

    // --- findByIdPuzzleIdAndIdWord ---

    @Test
    @DisplayName("findByIdPuzzleIdAndIdWord - Should return PuzzleWord when composite key matches")
    void shouldFindByIdPuzzleIdAndIdWord() {
        PuzzleWord pw = createPuzzleWord(defaultPuzzle, defaultDictWord, true);
        entityManager.persistAndFlush(pw);

        Optional<PuzzleWord> result = puzzleWordRepository.findByIdPuzzleIdAndIdWord(defaultPuzzle.getId(), "ALBERGO");

        assertThat(result).isPresent();
        assertThat(result.get().getDictionaryWord().getWord()).isEqualTo("ALBERGO");
    }

    @Test
    @DisplayName("findByIdPuzzleIdAndIdWord - Should return empty Optional when not found")
    void shouldReturnEmptyWhenPuzzleWordNotFound() {
        Optional<PuzzleWord> result = puzzleWordRepository.findByIdPuzzleIdAndIdWord(defaultPuzzle.getId(), "INEXISTENT");

        assertThat(result).isEmpty();
    }

    // --- countByIdPuzzleId ---

    @Test
    @DisplayName("countByIdPuzzleId - Should return correct word count for puzzle")
    void shouldCountByIdPuzzleId() {
        PuzzleWord pw = createPuzzleWord(defaultPuzzle, defaultDictWord, true);
        entityManager.persistAndFlush(pw);

        int count = puzzleWordRepository.countByIdPuzzleId(defaultPuzzle.getId());

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("countByIdPuzzleId - Should return zero when puzzle has no words")
    void shouldReturnZeroWhenPuzzleHasNoWords() {
        int count = puzzleWordRepository.countByIdPuzzleId("fake-puzzle-id");

        assertThat(count).isZero();
    }

    // --- deleteByIdWord ---

    @Test
    @DisplayName("deleteByIdWord - Should delete puzzle word by word id")
    void shouldDeleteByIdWord() {
        PuzzleWord pw = createPuzzleWord(defaultPuzzle, defaultDictWord, true);
        entityManager.persistAndFlush(pw);

        puzzleWordRepository.deleteByIdWord("ALBERGO");
        entityManager.flush();

        Optional<PuzzleWord> result = puzzleWordRepository.findByIdPuzzleIdAndIdWord(defaultPuzzle.getId(), "ALBERGO");
        assertThat(result).isEmpty();
    }

    // --- Helper Methods ---

    private User createUser(String username, String email) {
        return User.builder()
                .username(username)
                .email(email)
                .passwordHash("pwd")
                .build();
    }

    private DailyPuzzle createDailyPuzzle(LocalDate date, String centerLetter, int maxScore, String seed) {
        return DailyPuzzle.builder()
                .puzzleDate(date)
                .centerLetter(centerLetter)
                .maxScore(maxScore)
                .seed(seed)
                .build();
    }

    private DictionaryWord createDictionaryWord(String word, User user) {
        return DictionaryWord.builder()
                .word(word)
                .wordLength(word.length())
                .uniqueLettersCount((int) word.chars().distinct().count())
                .letterMask(182355)
                .isCandidatePangram(true)
                .addedByUser(user)
                .addedAt(new Date())
                .build();
    }

    private PuzzleWord createPuzzleWord(DailyPuzzle puzzle, DictionaryWord dictWord, boolean isMielegramma) {
        return PuzzleWord.builder()
                .id(new PuzzleWordId(puzzle.getId(), dictWord.getWord()))
                .puzzle(puzzle)
                .dictionaryWord(dictWord)
                .isMielegramma(isMielegramma)
                .build();
    }
}