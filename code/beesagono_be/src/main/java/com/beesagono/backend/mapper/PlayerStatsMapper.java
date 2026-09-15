package com.beesagono.backend.mapper;

import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.entity.PlayerSeason;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlayerStatsMapper {

    // PlayerSeason mapping
    @Mapping(target = "year", expression = "java(playerSeason.getId() != null ? playerSeason.getId().getSeasonYear() : null)")
    @Mapping(target = "gamesPlayed", ignore = true)
    @Mapping(target = "gamesCompleted", ignore = true)
    @Mapping(target = "currentStreak", ignore = true)
    @Mapping(target = "maxStreak", ignore = true)
    PlayerSeasonResponse toPlayerSeasonResponse(PlayerSeason playerSeason);
}