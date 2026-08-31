package com.beesagono.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSeasonResponse {

    private String userId;
    private Integer year;
    private Integer basePoints;
    private Integer bonusPoints;
    private Integer totalPoints;
    private String highestTierAchieved;
}
