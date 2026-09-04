package com.beesagono.backend.repository;

import com.beesagono.backend.entity.PlayerStats;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class PlayerStatsRepositoryTest {

    @Autowired
    private PlayerStatsRepository playerStatsRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("findById - Should find PlayerStats by user primary key")
    void shouldFindById() {
        User user = entityManager.persist(User.builder()
                .username("statsUser")
                .email("stats@example.com")
                .passwordHash("pwd")
                .build());

        PlayerStats stats = PlayerStats.builder()
                .user(user)
                .currentStreak(5)
                .maxStreak(10)
                .totalPoints(1200)
                .build();

        entityManager.persistAndFlush(stats);

        Optional<PlayerStats> found = playerStatsRepository.findById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCurrentStreak()).isEqualTo(5);
        assertThat(found.get().getTotalPoints()).isEqualTo(1200);
    }
}