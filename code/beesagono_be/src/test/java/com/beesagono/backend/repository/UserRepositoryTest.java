package com.beesagono.backend.repository;

import com.beesagono.backend.entity.User;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("findByUsername - Should find user by username")
    void shouldFindByUsername() {
        User user = User.builder()
                .username("mario")
                .email("mario@example.com")
                .passwordHash("secret")
                .build();
        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByUsername("mario");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("mario@example.com");
    }

    @Test
    @DisplayName("findByEmail - Should find user by email")
    void shouldFindByEmail() {
        User user = User.builder()
                .username("luigi")
                .email("luigi@example.com")
                .passwordHash("secret")
                .build();
        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByEmail("luigi@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("luigi");
    }

    @Test
    @DisplayName("findByUsernameOrEmail - Should find user by username or email")
    void shouldFindByUsernameOrEmail() {
        User user = User.builder()
                .username("peach")
                .email("peach@example.com")
                .passwordHash("secret")
                .build();
        entityManager.persistAndFlush(user);

        Optional<User> foundByUsername = userRepository.findByUsernameOrEmail("peach", "other@example.com");
        Optional<User> foundByEmail = userRepository.findByUsernameOrEmail("wrong", "peach@example.com");

        assertThat(foundByUsername).isPresent();
        assertThat(foundByEmail).isPresent();
    }

    @Test
    @DisplayName("existsByUsername - Should return true when username exists")
    void shouldReturnTrueWhenUsernameExists() {
        User user = User.builder()
                .username("yoshi")
                .email("yoshi@example.com")
                .passwordHash("secret")
                .build();
        entityManager.persistAndFlush(user);

        Boolean exists = userRepository.existsByUsername("yoshi");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByUsername - Should return false when username does not exist")
    void shouldReturnFalseWhenUsernameDoesNotExist() {
        Boolean exists = userRepository.existsByUsername("bowser");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByEmail - Should return true when email exists")
    void shouldReturnTrueWhenEmailExists() {
        User user = User.builder()
                .username("yoshi")
                .email("yoshi@example.com")
                .passwordHash("secret")
                .build();
        entityManager.persistAndFlush(user);

        Boolean exists = userRepository.existsByEmail("yoshi@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail - Should return false when email does not exist")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        Boolean exists = userRepository.existsByEmail("unknown@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase - Should find users matching search filter")
    void shouldFindUsersBySearchQuery() {
        User user1 = User.builder()
                .username("toad_admin")
                .email("toad@example.com")
                .passwordHash("secret")
                .build();

        User user2 = User.builder()
                .username("donkey_kong")
                .email("dk@kong.com")
                .passwordHash("secret")
                .build();

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        Page<User> result = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                "TOAD", "TOAD", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("toad_admin");
    }
}