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

    @Test
    @DisplayName("findByWord - Should return DictionaryWord when word exists")
    void shouldFindByWord() {
        DictionaryWord wordEntity = DictionaryWord.builder()
                .word("AFISE")
                .wordLength(5)
                .uniqueLettersCount(5)
                .isCandidatePangram(false)
                .addedByUser(adminUser)
                .addedAt(new Date())
                .build();

        entityManager.persistAndFlush(wordEntity);

        Optional<DictionaryWord> result = dictionaryWordRepository.findByWord("AFISE");

        assertThat(result).isPresent();
        assertThat(result.get().getWord()).isEqualTo("AFISE");
        assertThat(result.get().getWordLength()).isEqualTo(5);
        assertThat(result.get().getAddedByUser().getId()).isEqualTo(adminUser.getId());
    }

    @Test
    @DisplayName("findByWord - Should return empty Optional when word does not exist")
    void shouldReturnEmptyWhenWordNotFound() {
        Optional<DictionaryWord> result = dictionaryWordRepository.findByWord("NONESISTE");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByWord - Should return true when word exists")
    void shouldReturnTrueWhenExistsByWord() {
        DictionaryWord wordEntity = DictionaryWord.builder()
                .word("BEES")
                .wordLength(4)
                .uniqueLettersCount(3)
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
}