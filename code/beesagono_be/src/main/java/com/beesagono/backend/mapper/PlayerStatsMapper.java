package com.beesagono.backend.mapper;

import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.dto.stats.PlayerStatsResponse;
import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.PlayerStats;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlayerStatsMapper {

    // PlayerSeason mapping
    @Mapping(target = "year", expression = "java(playerSeason.getId() != null ? playerSeason.getId().getSeasonYear() : null)")
    PlayerSeasonResponse toPlayerSeasonResponse(PlayerSeason playerSeason);

    // PlayerStats mapping
    @Mapping(target = "averageScorePerGame", expression = "java(stats.getGamesPlayed() != null && stats.getGamesPlayed() > 0 ? Math.round(((double) (stats.getTotalScoreEarned() != null ? stats.getTotalScoreEarned() : 0) / stats.getGamesPlayed()) * 100.0) / 100.0 : 0.0)")
    @Mapping(target = "completionRate", expression = "java(stats.getGamesPlayed() != null && stats.getGamesPlayed() > 0 ? Math.round(((double) (stats.getGamesCompleted() != null ? stats.getGamesCompleted() : 0) / stats.getGamesPlayed()) * 100.0 * 100.0) / 100.0 : 0.0)")
    PlayerStatsResponse toPlayerStatsResponse(PlayerStats stats);
}