package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.PuzzleOuterLetter;
import com.beesagono.backend.entity.id.PuzzleOuterLetterId;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class PuzzleOuterLetterRepositoryTest {

    @Autowired
    private PuzzleOuterLetterRepository puzzleOuterLetterRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("findById - Should find PuzzleOuterLetter by composite primary key")
    void shouldFindById() {
        DailyPuzzle puzzle = entityManager.persist(DailyPuzzle.builder()
                .puzzleDate(LocalDate.now())
                .centerLetter("A")
                .maxScore(100)
                .seed("seed-outer")
                .build());

        PuzzleOuterLetterId id = new PuzzleOuterLetterId(puzzle.getId(), "B");
        PuzzleOuterLetter outerLetter = PuzzleOuterLetter.builder()
                .id(id)
                .puzzle(puzzle)
                .build();

        entityManager.persistAndFlush(outerLetter);

        Optional<PuzzleOuterLetter> found = puzzleOuterLetterRepository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getId().getLetter()).isEqualTo("B");
        assertThat(found.get().getPuzzle().getId()).isEqualTo(puzzle.getId());
    }

    @Test
    @DisplayName("findById - Should return empty Optional when outer letter not found")
    void shouldReturnEmptyWhenNotFound() {
        PuzzleOuterLetterId id = new PuzzleOuterLetterId("fake-puzzle-id", "Z");

        Optional<PuzzleOuterLetter> found = puzzleOuterLetterRepository.findById(id);

        assertThat(found).isEmpty();
    }
}