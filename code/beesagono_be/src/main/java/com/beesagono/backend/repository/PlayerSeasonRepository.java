package com.beesagono.backend.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.id.PlayerSeasonId;

import java.util.List;
import java.util.Optional;

public interface PlayerSeasonRepository extends JpaRepository<PlayerSeason, PlayerSeasonId> {

    List<PlayerSeason> findByUserId(String userId);

    /**
     * Retrieves a user's specific season using the composite key fields
     */
    Optional<PlayerSeason> findByIdUserIdAndIdSeasonYear(String userId, Integer seasonYear);

    /**
     * Retrieves top players for the year ordered by total points
     */
    @Query("SELECT ps FROM PlayerSeason ps WHERE ps.id.seasonYear = :year ORDER BY ps.totalPoints DESC")
    List<PlayerSeason> findTopPlayersByYear(@Param("year") int year, Pageable pageable);

    /**
     * Calculates the total number of players belonging to each rank tier for the
     * specified year
     */
    @Query("SELECT ps.highestTierAchieved, COUNT(ps) FROM PlayerSeason ps WHERE ps.id.seasonYear = :year GROUP BY ps.highestTierAchieved")
    List<Object[]> countPlayersByTierForYear(@Param("year") int year);
}