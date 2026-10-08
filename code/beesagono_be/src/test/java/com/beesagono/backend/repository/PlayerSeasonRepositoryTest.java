package com.beesagono.backend.repository;

import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.PlayerSeasonId;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class PlayerSeasonRepositoryTest {

        @Autowired
        private PlayerSeasonRepository playerSeasonRepository;

        @Autowired
        private TestEntityManager entityManager;

        private User user;

        @BeforeEach
        void setUp() {
                user = entityManager.persist(User.builder()
                                .username("seasonPlayer")
                                .email("season@example.com")
                                .passwordHash("pwd")
                                .build());
        }

        // --- findByUserId ---

        @Test
        @DisplayName("findByUserId - Should return list of seasons for a given userId")
        void shouldFindByUserId() {
                PlayerSeasonId id = new PlayerSeasonId(user.getId(), 2026);
                PlayerSeason season = PlayerSeason.builder()
                                .id(id)
                                .user(user)
                                .totalPoints(100)
                                .build();

                entityManager.persistAndFlush(season);

                List<PlayerSeason> seasons = playerSeasonRepository.findByUserId(user.getId());

                assertThat(seasons).hasSize(1);
        }

        @Test
        @DisplayName("findByUserId - Should return empty list when user has no seasons")
        void shouldReturnEmptyWhenUserHasNoSeasons() {
                List<PlayerSeason> seasons = playerSeasonRepository.findByUserId("nonexistent-user");

                assertThat(seasons).isEmpty();
        }

        // --- findByIdUserIdAndIdSeasonYear ---

        @Test
        @DisplayName("findByIdUserIdAndIdSeasonYear - Should return PlayerSeason by composite key attributes")
        void shouldFindByIdUserIdAndIdSeasonYear() {
                PlayerSeasonId id = new PlayerSeasonId(user.getId(), 2026);
                PlayerSeason season = PlayerSeason.builder()
                                .id(id)
                                .user(user)
                                .totalPoints(200)
                                .build();

                entityManager.persistAndFlush(season);

                Optional<PlayerSeason> found = playerSeasonRepository.findByIdUserIdAndIdSeasonYear(user.getId(), 2026);

                assertThat(found).isPresent();
                assertThat(found.get().getTotalPoints()).isEqualTo(200);
        }

        @Test
        @DisplayName("findByIdUserIdAndIdSeasonYear - Should return empty Optional when season not found")
        void shouldReturnEmptyWhenSeasonNotFound() {
                Optional<PlayerSeason> found = playerSeasonRepository.findByIdUserIdAndIdSeasonYear(user.getId(), 2099);

                assertThat(found).isEmpty();
        }
}