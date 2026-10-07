package com.beesagono.backend.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsSyncRequest {
    private Integer gamesPlayed;
    private Integer gamesCompleted;
    private Integer currentStreak;
    private Integer maxStreak;
    private String lastPlayedDate; // YYYY-MM-DD
    private List<Integer> claimedStreakMilestones;
    private Map<String, Integer> dailyRankDistribution;
}