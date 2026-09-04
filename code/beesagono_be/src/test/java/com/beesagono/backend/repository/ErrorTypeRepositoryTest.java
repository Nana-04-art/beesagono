package com.beesagono.backend.repository;

import com.beesagono.backend.entity.ErrorType;
import com.beesagono.backend.enums.ErrorTypeCode;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class ErrorTypeRepositoryTest {

    @Autowired
    private ErrorTypeRepository errorTypeRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("findById - Should find ErrorType by ErrorTypeCode enum key")
    void shouldFindById() {
        ErrorType errorType = ErrorType.builder()
                .code(ErrorTypeCode.NOT_IN_DICTIONARY)
                .description("La parola non è presente nel dizionario.")
                .build();

        entityManager.persistAndFlush(errorType);

        Optional<ErrorType> result = errorTypeRepository.findById(ErrorTypeCode.NOT_IN_DICTIONARY);

        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo(ErrorTypeCode.NOT_IN_DICTIONARY);
        assertThat(result.get().getDescription()).isEqualTo("La parola non è presente nel dizionario.");
    }

    @Test
    @DisplayName("findById - Should return empty when ErrorType is not persisted")
    void shouldReturnEmptyWhenNotFound() {
        Optional<ErrorType> result = errorTypeRepository.findById(ErrorTypeCode.TOO_SHORT);

        assertThat(result).isEmpty();
    }
}