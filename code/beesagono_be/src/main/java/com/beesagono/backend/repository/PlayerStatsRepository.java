package com.beesagono.backend.repository;

import com.beesagono.backend.entity.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerStatsRepository extends JpaRepository<PlayerStats, String> {
    Optional<PlayerStats> findByUserId(String userId);
}