package com.beesagono.backend.repository;

import com.beesagono.backend.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, String> {
}