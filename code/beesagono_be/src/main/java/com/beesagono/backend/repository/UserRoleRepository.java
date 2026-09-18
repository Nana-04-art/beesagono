package com.beesagono.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beesagono.backend.entity.UserRole;
import com.beesagono.backend.entity.id.UserRoleId;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}