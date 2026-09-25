package com.beesagono.backend.dto.badge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object representing the response payload for a badge,
 * including its metadata, unlock status, timestamp, and progress metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeResponse {
    private String code;
    private String title;
    private String description;
    private String iconUrl;
    private String category;
    private boolean unlocked;
    private Instant unlockedAt;
    private Integer currentProgress;
    private Integer targetValue;
    private Double progressPercentage;
}