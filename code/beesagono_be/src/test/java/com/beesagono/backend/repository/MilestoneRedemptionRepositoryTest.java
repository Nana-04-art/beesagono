package com.beesagono.backend.repository;

import com.beesagono.backend.entity.MilestoneRedemption;
import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.MilestoneRedemptionId;
import com.beesagono.backend.entity.id.PlayerSeasonId;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class MilestoneRedemptionRepositoryTest {

    @Autowired
    private MilestoneRedemptionRepository milestoneRedemptionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("findByIdUserIdAndIdSeasonYear - Should return list of redemptions for given userId and season year")
    void shouldFindByIdUserIdAndIdSeasonYear() {
        User user = entityManager.persist(User.builder()
                .username("redemptionPlayer")
                .email("redemption@example.com")
                .passwordHash("pwd")
                .build());

        PlayerSeasonId seasonId = new PlayerSeasonId(user.getId(), 2026);
        PlayerSeason playerSeason = entityManager.persist(PlayerSeason.builder()
                .id(seasonId)
                .user(user)
                .totalPoints(500)
                .build());

        MilestoneRedemptionId redemptionId = new MilestoneRedemptionId(user.getId(), 2026, 5);

        MilestoneRedemption redemption = MilestoneRedemption.builder()
                .id(redemptionId)
                .user(user)
                .playerSeason(playerSeason)
                .build();

        entityManager.persistAndFlush(redemption);

        List<MilestoneRedemption> results = milestoneRedemptionRepository
                .findByIdUserIdAndIdSeasonYear(user.getId(), 2026);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId().getUserId()).isEqualTo(user.getId());
        assertThat(results.get(0).getId().getSeasonYear()).isEqualTo(2026);
        assertThat(results.get(0).getId().getStreakLength()).isEqualTo(5);
    }
}