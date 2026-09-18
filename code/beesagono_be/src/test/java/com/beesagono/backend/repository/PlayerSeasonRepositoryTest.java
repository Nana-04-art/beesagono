package com.beesagono.backend.repository;

import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.PlayerSeasonId;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class PlayerSeasonRepositoryTest {

    @Autowired
    private PlayerSeasonRepository playerSeasonRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = entityManager.persist(User.builder()
                .username("seasonPlayer1")
                .email("season1@example.com")
                .passwordHash("pwd")
                .build());

        user2 = entityManager.persist(User.builder()
                .username("seasonPlayer2")
                .email("season2@example.com")
                .passwordHash("pwd")
                .build());
    }

    // --- findByUserId ---

    @Nested
    @DisplayName("findByUserId Tests")
    class FindByUserIdTests {

        @Test
        @DisplayName("findByUserId - Should return list of seasons for a given userId")
        void shouldFindByUserId() {
            PlayerSeasonId id = new PlayerSeasonId(user1.getId(), 2026);
            PlayerSeason season = PlayerSeason.builder()
                    .id(id)
                    .user(user1)
                    .totalPoints(100)
                    .build();

            entityManager.persistAndFlush(season);

            List<PlayerSeason> seasons = playerSeasonRepository.findByUserId(user1.getId());

            assertThat(seasons).hasSize(1);
            assertThat(seasons.get(0).getId().getSeasonYear()).isEqualTo(2026);
        }

        @Test
        @DisplayName("findByUserId - Should return empty list when user has no seasons")
        void shouldReturnEmptyWhenUserHasNoSeasons() {
            List<PlayerSeason> seasons = playerSeasonRepository.findByUserId("nonexistent-user");

            assertThat(seasons).isEmpty();
        }
    }

    // --- findByIdUserIdAndIdSeasonYear ---

    @Nested
    @DisplayName("findByIdUserIdAndIdSeasonYear Tests")
    class FindByIdUserIdAndIdSeasonYearTests {

        @Test
        @DisplayName("findByIdUserIdAndIdSeasonYear - Should return PlayerSeason by composite key attributes")
        void shouldFindByIdUserIdAndIdSeasonYear() {
            PlayerSeasonId id = new PlayerSeasonId(user1.getId(), 2026);
            PlayerSeason season = PlayerSeason.builder()
                    .id(id)
                    .user(user1)
                    .totalPoints(200)
                    .build();

            entityManager.persistAndFlush(season);

            Optional<PlayerSeason> found = playerSeasonRepository.findByIdUserIdAndIdSeasonYear(user1.getId(), 2026);

            assertThat(found).isPresent();
            assertThat(found.get().getTotalPoints()).isEqualTo(200);
        }

        @Test
        @DisplayName("findByIdUserIdAndIdSeasonYear - Should return empty Optional when season not found")
        void shouldReturnEmptyWhenSeasonNotFound() {
            Optional<PlayerSeason> found = playerSeasonRepository.findByIdUserIdAndIdSeasonYear(user1.getId(), 2099);

            assertThat(found).isEmpty();
        }
    }

    // --- findTopPlayersByYear ---

    @Nested
    @DisplayName("findTopPlayersByYear Tests")
    class FindTopPlayersByYearTests {

        @Test
        @DisplayName("findTopPlayersByYear - Should return players sorted by totalPoints DESC with pagination limit")
        void shouldFindTopPlayersByYearInOrder() {
            PlayerSeason season1 = PlayerSeason.builder()
                    .id(new PlayerSeasonId(user1.getId(), 2026))
                    .user(user1)
                    .totalPoints(150)
                    .build();

            PlayerSeason season2 = PlayerSeason.builder()
                    .id(new PlayerSeasonId(user2.getId(), 2026))
                    .user(user2)
                    .totalPoints(300)
                    .build();

            entityManager.persist(season1);
            entityManager.persist(season2);
            entityManager.flush();

            List<PlayerSeason> topPlayers = playerSeasonRepository.findTopPlayersByYear(2026, PageRequest.of(0, 10));

            assertThat(topPlayers).hasSize(2);
            assertThat(topPlayers.get(0).getTotalPoints()).isEqualTo(300); // Prima user2 con 300 pt
            assertThat(topPlayers.get(1).getTotalPoints()).isEqualTo(150); // Poi user1 con 150 pt
        }

        @Test
        @DisplayName("findTopPlayersByYear - Should respects pageable limit")
        void shouldRespectPageableLimit() {
            PlayerSeason season1 = PlayerSeason.builder()
                    .id(new PlayerSeasonId(user1.getId(), 2026))
                    .user(user1)
                    .totalPoints(150)
                    .build();

            PlayerSeason season2 = PlayerSeason.builder()
                    .id(new PlayerSeasonId(user2.getId(), 2026))
                    .user(user2)
                    .totalPoints(300)
                    .build();

            entityManager.persist(season1);
            entityManager.persist(season2);
            entityManager.flush();

            List<PlayerSeason> topPlayers = playerSeasonRepository.findTopPlayersByYear(2026, PageRequest.of(0, 1));

            assertThat(topPlayers).hasSize(1);
            assertThat(topPlayers.get(0).getTotalPoints()).isEqualTo(300);
        }
    }

    // --- countPlayersByTierForYear ---

    @Nested
    @DisplayName("countPlayersByTierForYear Tests")
    class CountPlayersByTierForYearTests {

        @Test
        @DisplayName("countPlayersByTierForYear - Should aggregate player count grouped by tier")
        void shouldCountPlayersByTierForYear() {
            PlayerSeason season1 = PlayerSeason.builder()
                    .id(new PlayerSeasonId(user1.getId(), 2026))
                    .user(user1)
                    .highestTierAchieved("Uovo d'Ape")
                    .totalPoints(40)
                    .build();

            PlayerSeason season2 = PlayerSeason.builder()
                    .id(new PlayerSeasonId(user2.getId(), 2026))
                    .user(user2)
                    .highestTierAchieved("Uovo d'Ape")
                    .totalPoints(45)
                    .build();

            entityManager.persist(season1);
            entityManager.persist(season2);
            entityManager.flush();

            List<Object[]> results = playerSeasonRepository.countPlayersByTierForYear(2026);

            assertThat(results).hasSize(1);
            Object[] firstRow = results.get(0);
            assertThat(firstRow[0]).isEqualTo("Uovo d'Ape");
            assertThat(firstRow[1]).isEqualTo(2L); // 2 utenti nella stessa fascia
        }

        @Test
        @DisplayName("countPlayersByTierForYear - Should return empty list when no players in year")
        void shouldReturnEmptyListWhenNoDataForYear() {
            List<Object[]> results = playerSeasonRepository.countPlayersByTierForYear(1999);

            assertThat(results).isEmpty();
        }
    }
}