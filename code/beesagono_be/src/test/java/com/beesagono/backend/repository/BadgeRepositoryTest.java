package com.beesagono.backend.repository;

import com.beesagono.backend.entity.Badge;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class BadgeRepositoryTest {

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Badge badge1;
    private Badge badge2;

    @BeforeEach
    void setUp() {
        badge1 = Badge.builder()
                .code("FIRST_WIN")
                .title("Prima Vittoria")
                .description("Hai vinto la tua prima partita")
                .iconUrl("http://example.com/win.png")
                .category("WIN")
                .build();

        badge2 = Badge.builder()
                .code("STREAK_7")
                .title("Serie di 7 Giorni")
                .description("Gioca per 7 giorni consecutivi")
                .iconUrl("http://example.com/streak.png")
                .category("STREAK")
                .build();
    }

    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("findById - Should return badge when exists")
        void shouldFindByIdWhenExists() {
            entityManager.persistAndFlush(badge1);

            Optional<Badge> found = badgeRepository.findById("FIRST_WIN");

            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Prima Vittoria");
            assertThat(found.get().getCategory()).isEqualTo("WIN");
        }

        @Test
        @DisplayName("findById - Should return empty Optional when badge code does not exist")
        void shouldReturnEmptyWhenBadgeNotFound() {
            Optional<Badge> found = badgeRepository.findById("NON_EXISTENT_CODE");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll Tests")
    class FindAllTests {

        @Test
        @DisplayName("findAll - Should return list of all persisted badges")
        void shouldFindAllBadges() {
            entityManager.persist(badge1);
            entityManager.persist(badge2);
            entityManager.flush();

            List<Badge> badges = badgeRepository.findAll();

            assertThat(badges).hasSize(2);
            assertThat(badges).extracting(Badge::getCode).containsExactlyInAnyOrder("FIRST_WIN", "STREAK_7");
        }

        @Test
        @DisplayName("findAll - Should return empty list when no badges exist")
        void shouldReturnEmptyListWhenNoBadges() {
            List<Badge> badges = badgeRepository.findAll();

            assertThat(badges).isEmpty();
        }
    }
}