package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DictionaryWord;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class DictionaryWordRepositoryTest {

    @Autowired
    private DictionaryWordRepository dictionaryWordRepository;

    @Test
    @DisplayName("findByWord - Should return DictionaryWord when word exists")
    void shouldFindByWord() {
        DictionaryWord word = DictionaryWord.builder()
                .word("ALVEARE")
                .build();
        dictionaryWordRepository.save(word);

        Optional<DictionaryWord> found = dictionaryWordRepository.findByWord("ALVEARE");

        assertThat(found).isPresent();
        assertThat(found.get().getWord()).isEqualTo("ALVEARE");
    }

    @Test
    @DisplayName("existsByWord - Should return true when word exists")
    void shouldReturnTrueWhenWordExists() {
        DictionaryWord word = DictionaryWord.builder()
                .word("AFTOSI")
                .build();
        dictionaryWordRepository.save(word);

        Boolean exists = dictionaryWordRepository.existsByWord("AFTOSI");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByWord - Should return false when word does not exist")
    void shouldReturnFalseWhenWordDoesNotExist() {
        Boolean exists = dictionaryWordRepository.existsByWord("INESISTENTE");

        assertThat(exists).isFalse();
    }
}