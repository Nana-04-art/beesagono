package com.beesagono.backend.repository;

import com.beesagono.backend.entity.RefreshToken;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("findByTokenHash - Should return RefreshToken when tokenHash exists")
    void shouldFindByTokenHash() {
        User user = entityManager.persist(User.builder()
                .username("tokenUser")
                .email("token@example.com")
                .passwordHash("pwd")
                .build());

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash("hash-xyz-123")
                .expiresAt(new Date(System.currentTimeMillis() + 86400000))
                .build();

        entityManager.persistAndFlush(token);

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("hash-xyz-123");

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getUsername()).isEqualTo("tokenUser");
    }

    @Test
    @DisplayName("deleteByUser - Should delete all refresh tokens for a given user")
    void shouldDeleteByUser() {
        User user = entityManager.persist(User.builder()
                .username("deleteUser")
                .email("delete@example.com")
                .passwordHash("pwd")
                .build());

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash("hash-to-delete")
                .expiresAt(new Date(System.currentTimeMillis() + 86400000))
                .build();

        entityManager.persistAndFlush(token);

        int deletedCount = refreshTokenRepository.deleteByUser(user);
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("hash-to-delete");

        assertThat(deletedCount).isEqualTo(1);
        assertThat(found).isEmpty();
    }
}