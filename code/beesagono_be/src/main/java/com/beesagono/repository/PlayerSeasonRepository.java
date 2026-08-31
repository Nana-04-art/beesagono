package com.beesagono.repository;

import com.beesagono.entity.PlayerSeason;
import com.beesagono.entity.PlayerSeason.PlayerSeasonId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerSeasonRepository extends JpaRepository<PlayerSeason, PlayerSeasonId> {
    List<PlayerSeason> findByUserId(String userId);

    Optional<PlayerSeason> findByUserIdAndYear(String userId, Integer year);
}