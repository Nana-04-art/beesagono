package com.beesagono.backend.repository;

import com.beesagono.backend.entity.Role;
import com.beesagono.backend.enums.RoleName;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("findByName - Should return Role when role name exists")
    void shouldFindByName() {
        Role role = Role.builder()
                .name(RoleName.ROLE_USER)
                .build();

        entityManager.persistAndFlush(role);

        Optional<Role> found = roleRepository.findByName(RoleName.ROLE_USER);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo(RoleName.ROLE_USER);
    }
}