package com.beesagono.backend.repository;

import com.beesagono.backend.entity.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerStatsRepository extends JpaRepository<PlayerStats, String> {
}