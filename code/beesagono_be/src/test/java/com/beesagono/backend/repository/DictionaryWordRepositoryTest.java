package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DictionaryWord;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class DictionaryWordRepositoryTest {

    @Autowired
    private DictionaryWordRepository dictionaryWordRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = entityManager.persist(User.builder()
                .username("dictAdmin")
                .email("dictadmin@example.com")
                .passwordHash("hashedPwd")
                .build());
    }

    // --- findByWord ---

    @Test
    @DisplayName("findByWord - Should return DictionaryWord when word exists")
    void shouldFindByWord() {
        DictionaryWord wordEntity = DictionaryWord.builder()
                .word("AFISE")
                .wordLength(5)
                .uniqueLettersCount(5)
                .letterMask(100)
                .isCandidatePangram(false)
                .addedByUser(adminUser)
                .addedAt(new Date())
                .build();

        entityManager.persistAndFlush(wordEntity);

        Optional<DictionaryWord> result = dictionaryWordRepository.findByWord("AFISE");

        assertThat(result).isPresent();
        assertThat(result.get().getWord()).isEqualTo("AFISE");
    }

    @Test
    @DisplayName("findByWord - Should return empty Optional when word does not exist")
    void shouldReturnEmptyWhenWordNotFound() {
        Optional<DictionaryWord> result = dictionaryWordRepository.findByWord("NONESISTE");

        assertThat(result).isEmpty();
    }

    // --- existsByWord ---

    @Test
    @DisplayName("existsByWord - Should return true when word exists")
    void shouldReturnTrueWhenExistsByWord() {
        DictionaryWord wordEntity = DictionaryWord.builder()
                .word("BEES")
                .wordLength(4)
                .uniqueLettersCount(3)
                .letterMask(200)
                .isCandidatePangram(false)
                .addedByUser(adminUser)
                .addedAt(new Date())
                .build();

        entityManager.persistAndFlush(wordEntity);

        Boolean exists = dictionaryWordRepository.existsByWord("BEES");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByWord - Should return false when word does not exist")
    void shouldReturnFalseWhenNotExistsByWord() {
        Boolean exists = dictionaryWordRepository.existsByWord("INEXISTENT");

        assertThat(exists).isFalse();
    }

    // --- findCandidatePangrams ---

    @Test
    @DisplayName("findCandidatePangrams - Should return list of candidate pangram words")
    void shouldFindCandidatePangrams() {
        entityManager.persist(createWord("ALBERGO", 7, 7, 182355, true));
        entityManager.persist(createWord("LAGO", 4, 4, 100, false));
        entityManager.flush();

        List<String> pangrams = dictionaryWordRepository.findCandidatePangrams();

        assertThat(pangrams).hasSize(1);
        assertThat(pangrams.get(0)).isEqualTo("ALBERGO");
    }

    @Test
    @DisplayName("findCandidatePangrams - Should return empty list when no candidate pangrams exist")
    void shouldReturnEmptyWhenNoCandidatePangramsExist() {
        DictionaryWord normalWord = DictionaryWord.builder()
                .word("LAGO")
                .wordLength(4)
                .uniqueLettersCount(4)
                .letterMask(100)
                .isCandidatePangram(false)
                .addedByUser(adminUser)
                .addedAt(new Date())
                .build();

        entityManager.persistAndFlush(normalWord);

        List<String> pangrams = dictionaryWordRepository.findCandidatePangrams();

        assertThat(pangrams).isEmpty();
    }

    // --- findValidWordsForPuzzle ---

    @Test
    @DisplayName("findValidWordsForPuzzle - Should return words matching center bit and puzzle mask")
    void shouldFindValidWordsForPuzzle() {
        entityManager.persist(createWord("ALBERGO", 7, 7, 182355, true));
        entityManager.flush();

        List<DictionaryWord> validWords = dictionaryWordRepository.findValidWordsForPuzzle(1, 182355);

        assertThat(validWords).hasSize(1);
        assertThat(validWords.get(0).getWord()).isEqualTo("ALBERGO");
    }

    @Test
    @DisplayName("findValidWordsForPuzzle - Should exclude words with invalid mask or missing center letter")
    void shouldExcludeInvalidWordsForPuzzle() {
        entityManager.persist(createWord("PAPA", 4, 2, 1048577, false));
        entityManager.persist(createWord("LORO", 4, 3, 1024, false));
        entityManager.flush();

        List<DictionaryWord> validWords = dictionaryWordRepository.findValidWordsForPuzzle(1, 182355);

        assertThat(validWords).isEmpty();
    }

    private DictionaryWord createWord(String word, int length, int unique, int mask, boolean isPangram) {
        return DictionaryWord.builder()
                .word(word)
                .wordLength(length)
                .uniqueLettersCount(unique)
                .letterMask(mask)
                .isCandidatePangram(isPangram)
                .addedByUser(adminUser)
                .addedAt(new Date())
                .build();
    }
}