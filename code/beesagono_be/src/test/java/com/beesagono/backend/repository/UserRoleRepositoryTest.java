package com.beesagono.backend.repository;

import com.beesagono.backend.entity.Role;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.UserRole;
import com.beesagono.backend.entity.id.UserRoleId;
import com.beesagono.backend.enums.RoleName;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class UserRoleRepositoryTest {

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("findById - Should find UserRole by composite primary key")
    void shouldFindById() {
        User user = entityManager.persist(User.builder()
                .username("roleUser")
                .email("role@example.com")
                .passwordHash("pwd")
                .build());

        Role role = entityManager.persist(Role.builder()
                .name(RoleName.ROLE_USER)
                .build());

        UserRoleId userRoleId = new UserRoleId(user.getId(), role.getId());
        UserRole userRole = UserRole.builder()
                .id(userRoleId)
                .user(user)
                .role(role)
                .build();

        entityManager.persistAndFlush(userRole);

        Optional<UserRole> found = userRoleRepository.findById(userRoleId);

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getUsername()).isEqualTo("roleUser");
        assertThat(found.get().getRole().getName()).isEqualTo(RoleName.ROLE_USER);
    }

    @Test
    @DisplayName("findById - Should return empty Optional when user role association not found")
    void shouldReturnEmptyWhenNotFound() {
        UserRoleId userRoleId = new UserRoleId("fake-user-id", "fake-role-id");

        Optional<UserRole> found = userRoleRepository.findById(userRoleId);

        assertThat(found).isEmpty();
    }
}