package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.testsupport.H2DataJpaTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class DailyPuzzleRepositoryTest {

    @Autowired
    private DailyPuzzleRepository dailyPuzzleRepository;

    @Test
    @DisplayName("findByPuzzleDate - Should find DailyPuzzle by puzzleDate")
    void shouldFindByPuzzleDate() {
        LocalDate today = LocalDate.now();
        DailyPuzzle puzzle = DailyPuzzle.builder()
                .puzzleDate(today)
                .centerLetter("A")
                .maxScore(100)
                .seed("seed-123")
                .build();
        dailyPuzzleRepository.save(puzzle);

        Optional<DailyPuzzle> found = dailyPuzzleRepository.findByPuzzleDate(today);

        assertThat(found).isPresent();
        assertThat(found.get().getCenterLetter()).isEqualTo("A");
    }
}