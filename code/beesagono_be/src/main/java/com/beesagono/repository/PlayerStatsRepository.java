package com.beesagono.repository;

import com.beesagono.entity.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerStatsRepository extends JpaRepository<PlayerStats, String> {
}