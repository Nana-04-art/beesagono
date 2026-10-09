package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class DailyPuzzleRepositoryTest {

    @Autowired
    private DailyPuzzleRepository dailyPuzzleRepository;

    @Autowired
    private TestEntityManager entityManager;

    // --- findByPuzzleDate ---

    @Test
    @DisplayName("findByPuzzleDate - Should find DailyPuzzle by puzzleDate")
    void shouldFindByPuzzleDate() {
        LocalDate today = LocalDate.now();
        DailyPuzzle puzzle = createDailyPuzzle(today, "A", 100, "seed-123");
        entityManager.persistAndFlush(puzzle);

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
        DailyPuzzle puzzle = createDailyPuzzle(today, "A", 100, "seed-123");
        entityManager.persistAndFlush(puzzle);

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
        DailyPuzzle p1 = createDailyPuzzle(LocalDate.of(2026, 9, 1), "A", 100, "seed-1");
        DailyPuzzle p2 = createDailyPuzzle(LocalDate.of(2026, 9, 2), "B", 100, "seed-2");

        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.flush();

        List<DailyPuzzle> results = dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc(Pageable.unpaged());

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getPuzzleDate()).isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(results.get(1).getPuzzleDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    // --- findAllByPuzzleDateAfter ---

    @Test
    @DisplayName("findAllByPuzzleDateAfter - Should return puzzles with date strictly after specified date")
    void shouldFindAllByPuzzleDateAfter() {
        LocalDate baseDate = LocalDate.of(2026, 10, 1);
        DailyPuzzle past = createDailyPuzzle(baseDate.minusDays(1), "X", 80, "seed-past");
        DailyPuzzle current = createDailyPuzzle(baseDate, "Y", 90, "seed-current");
        DailyPuzzle future1 = createDailyPuzzle(baseDate.plusDays(1), "Z", 100, "seed-f1");
        DailyPuzzle future2 = createDailyPuzzle(baseDate.plusDays(5), "W", 110, "seed-f2");

        entityManager.persist(past);
        entityManager.persist(current);
        entityManager.persist(future1);
        entityManager.persist(future2);
        entityManager.flush();

        List<DailyPuzzle> futurePuzzles = dailyPuzzleRepository.findAllByPuzzleDateAfter(baseDate);

        assertThat(futurePuzzles).hasSize(2);
        assertThat(futurePuzzles)
                .extracting(DailyPuzzle::getPuzzleDate)
                .containsExactlyInAnyOrder(baseDate.plusDays(1), baseDate.plusDays(5));
    }

    // --- Helper Methods ---

    private DailyPuzzle createDailyPuzzle(LocalDate date, String centerLetter, int maxScore, String seed) {
        return DailyPuzzle.builder()
                .puzzleDate(date)
                .centerLetter(centerLetter)
                .maxScore(maxScore)
                .seed(seed)
                .build();
    }
}