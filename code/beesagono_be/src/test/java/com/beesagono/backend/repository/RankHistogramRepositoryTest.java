package com.beesagono.backend.repository;

import com.beesagono.backend.entity.RankHistogram;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.RankHistogramId;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class RankHistogramRepositoryTest {

    @Autowired
    private RankHistogramRepository rankHistogramRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("findByIdUserId - Should return list of rank histograms for a given userId")
    void shouldFindByIdUserId() {
        User user = entityManager.persist(User.builder()
                .username("rankUser")
                .email("rank@example.com")
                .passwordHash("pwd")
                .build());

        RankHistogramId id = new RankHistogramId(user.getId(), "Genio");
        RankHistogram histogram = RankHistogram.builder()
                .id(id)
                .user(user)
                .count(3)
                .build();

        entityManager.persistAndFlush(histogram);

        List<RankHistogram> results = rankHistogramRepository.findByIdUserId(user.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId().getRankLabel()).isEqualTo("Genio");
        assertThat(results.get(0).getCount()).isEqualTo(3);
    }
}