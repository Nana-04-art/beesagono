package com.beesagono.backend.repository;

import com.beesagono.backend.entity.Badge;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.UserBadge;
import com.beesagono.backend.entity.id.UserBadgeId;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class UserBadgeRepositoryTest {

    @Autowired
    private UserBadgeRepository userBadgeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Badge badge;

    @BeforeEach
    void setUp() {
        user = entityManager.persist(User.builder()
                .username("badgePlayer")
                .email("badgeplayer@example.com")
                .passwordHash("pwd")
                .build());

        badge = entityManager.persist(Badge.builder()
                .code("FIRST_WIN")
                .title("Prima Vittoria")
                .description("Completa la tua prima partita")
                .category("WIN")
                .build());
    }

    @Nested
    @DisplayName("findByIdUserId Tests")
    class FindByIdUserIdTests {

        @Test
        @DisplayName("findByIdUserId - Should return user badges for a given userId")
        void shouldFindByIdUserId() {
            UserBadge userBadge = UserBadge.builder()
                    .id(new UserBadgeId(user.getId(), badge.getCode()))
                    .user(user)
                    .badge(badge)
                    .unlockedAt(Instant.now())
                    .build();

            entityManager.persistAndFlush(userBadge);

            List<UserBadge> userBadges = userBadgeRepository.findByIdUserId(user.getId());

            assertThat(userBadges).hasSize(1);
            assertThat(userBadges.get(0).getId().getBadgeCode()).isEqualTo("FIRST_WIN");
        }

        @Test
        @DisplayName("findByIdUserId - Should return empty list when user has no badges")
        void shouldReturnEmptyWhenUserHasNoBadges() {
            List<UserBadge> userBadges = userBadgeRepository.findByIdUserId("nonexistent-user");

            assertThat(userBadges).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByIdUserIdAndIdBadgeCode Tests")
    class ExistsByIdUserIdAndIdBadgeCodeTests {

        @Test
        @DisplayName("existsByIdUserIdAndIdBadgeCode - Should return true when user badge exists")
        void shouldReturnTrueWhenBadgeExists() {
            UserBadge userBadge = UserBadge.builder()
                    .id(new UserBadgeId(user.getId(), badge.getCode()))
                    .user(user)
                    .badge(badge)
                    .unlockedAt(Instant.now())
                    .build();

            entityManager.persistAndFlush(userBadge);

            boolean exists = userBadgeRepository.existsByIdUserIdAndIdBadgeCode(user.getId(), badge.getCode());

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("existsByIdUserIdAndIdBadgeCode - Should return false when user badge does not exist")
        void shouldReturnFalseWhenBadgeDoesNotExist() {
            boolean exists = userBadgeRepository.existsByIdUserIdAndIdBadgeCode(user.getId(), "NON_EXISTENT_BADGE");

            assertThat(exists).isFalse();
        }
    }
}