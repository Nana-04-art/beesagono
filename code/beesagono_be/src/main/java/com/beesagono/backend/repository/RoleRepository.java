package com.beesagono.backend.repository;

import com.beesagono.backend.entity.Role;
import com.beesagono.backend.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {
    Optional<Role> findByName(RoleName name);
}