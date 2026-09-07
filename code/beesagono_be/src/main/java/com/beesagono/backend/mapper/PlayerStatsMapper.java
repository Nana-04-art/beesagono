package com.beesagono.backend.mapper;

import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.dto.stats.PlayerStatsResponse;
import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.PlayerStats;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlayerStatsMapper {

    // PlayerStat mapping
    @Mapping(target = "userId", source = "userId")
    PlayerStatsResponse toPlayerStatsResponse(PlayerStats playerStats);

    // PlayerSeason mapping
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "year", expression = "java(playerSeason.getId() != null ? playerSeason.getId().getSeasonYear() : null)")
    PlayerSeasonResponse toPlayerSeasonResponse(PlayerSeason playerSeason);
}