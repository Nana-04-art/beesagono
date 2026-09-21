package com.beesagono.backend.dto.badge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object representing the response payload for a badge,
 * including its metadata and the user's unlock status and timestamp.
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
}