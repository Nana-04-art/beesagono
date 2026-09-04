package com.beesagono.backend.repository;

import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.PlayerSeasonId;
import com.beesagono.backend.testsupport.H2DataJpaTest;
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

    @Test
    @DisplayName("findByUserId - Should return list of seasons for a given userId")
    void shouldFindByUserId() {
        User user = entityManager.persist(User.builder()
                .username("seasonPlayer1")
                .email("season1@example.com")
                .passwordHash("pwd")
                .build());

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
    @DisplayName("findByIdUserIdAndIdYear - Should return PlayerSeason by composite key attributes")
    void shouldFindByIdUserIdAndIdYear() {
        User user = entityManager.persist(User.builder()
                .username("seasonPlayer2")
                .email("season2@example.com")
                .passwordHash("pwd")
                .build());

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
}