package com.beesagono.backend.mapper;

import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.dto.stats.PlayerStatsResponse;
import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.PlayerStats;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlayerStatsMapper {

    @Mapping(target = "userId", source = "user.id")
    PlayerStatsResponse toPlayerStatsResponse(PlayerStats playerStats);

    @Mapping(target = "userId", source = "id.userId")
    @Mapping(target = "year", source = "id.year")
    PlayerSeasonResponse toPlayerSeasonResponse(PlayerSeason playerSeason);
}