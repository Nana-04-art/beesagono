package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class DailyPuzzleRepositoryTest {

    @Autowired
    private DailyPuzzleRepository dailyPuzzleRepository;

    // --- findByPuzzleDate ---

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

    @Test
    @DisplayName("findByPuzzleDate - Should return empty Optional when date does not exist")
    void shouldReturnEmptyWhenPuzzleDateNotFound() {
        Optional<DailyPuzzle> found = dailyPuzzleRepository.findByPuzzleDate(LocalDate.of(2099, 1, 1));

        assertThat(found).isEmpty();
    }

    // --- existsByPuzzleDate ---

    @Test
    @DisplayName("existsByPuzzleDate - Should return true when puzzle exists for date")
    void shouldReturnTrueWhenExistsByPuzzleDate() {
        LocalDate today = LocalDate.now();
        DailyPuzzle puzzle = DailyPuzzle.builder()
                .puzzleDate(today)
                .centerLetter("A")
                .maxScore(100)
                .seed("seed-123")
                .build();
        dailyPuzzleRepository.save(puzzle);

        boolean exists = dailyPuzzleRepository.existsByPuzzleDate(today);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByPuzzleDate - Should return false when puzzle does not exist for date")
    void shouldReturnFalseWhenNotExistsByPuzzleDate() {
        boolean exists = dailyPuzzleRepository.existsByPuzzleDate(LocalDate.of(2099, 1, 1));

        assertThat(exists).isFalse();
    }

    // --- findAllByOrderByPuzzleDateDesc ---

    @Test
    @DisplayName("findAllByOrderByPuzzleDateDesc - Should return puzzles ordered by date descending")
    void shouldFindAllByOrderByPuzzleDateDesc() {
        DailyPuzzle p1 = DailyPuzzle.builder()
                .puzzleDate(LocalDate.of(2026, 9, 1))
                .centerLetter("A")
                .maxScore(100)
                .seed("seed-1")
                .build();
        DailyPuzzle p2 = DailyPuzzle.builder()
                .puzzleDate(LocalDate.of(2026, 9, 2))
                .centerLetter("B")
                .maxScore(100)
                .seed("seed-2")
                .build();

        dailyPuzzleRepository.save(p1);
        dailyPuzzleRepository.save(p2);

        List<DailyPuzzle> results = dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc(PageRequest.of(0, 10));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getPuzzleDate()).isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(results.get(1).getPuzzleDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    }
}