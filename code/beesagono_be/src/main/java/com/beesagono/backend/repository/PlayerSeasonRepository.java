package com.beesagono.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.id.PlayerSeasonId;

import java.util.List;
import java.util.Optional;

public interface PlayerSeasonRepository extends JpaRepository<PlayerSeason, PlayerSeasonId> {
    List<PlayerSeason> findByUserId(String userId);

    Optional<PlayerSeason> findByIdUserIdAndIdYear(String userId, Integer year);
}