package com.beesagono.backend.entity.id;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key for tracking streak milestone reward redemptions per user, season, and streak milestone length.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class MilestoneRedemptionId implements Serializable {

    @Column(name = "user_id")
    private String userId;

    @Column(name = "season_year")
    private Integer seasonYear;

    @Column(name = "streak_length")
    private Integer streakLength;
}