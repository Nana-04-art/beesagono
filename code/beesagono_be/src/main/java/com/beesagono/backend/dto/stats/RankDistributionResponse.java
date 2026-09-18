package com.beesagono.backend.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Data Transfer Object representing community rank distribution data for a specific season,
 * mapping tier labels to player counts for statistical insights.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankDistributionResponse {
    private int seasonYear;
    private long totalPlayers;
    private Map<String, Long> tierCounts; // e.g. : {"Regina": 12, "Ape Operosa": 45, ...}
}