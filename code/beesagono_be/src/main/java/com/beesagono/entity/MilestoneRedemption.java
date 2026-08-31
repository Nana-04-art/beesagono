package com.beesagono.entity;

import java.io.Serializable;
import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "milestone_redemptions")
@SuperBuilder
public class MilestoneRedemption {

    @EmbeddedId
    private MilestoneRedemptionId id;

    @ManyToOne
    @MapsId("playerSeasonId")
    @JoinColumns({
        @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
        @JoinColumn(name = "year", referencedColumnName = "year")
    })
    private PlayerSeason playerSeason;

    @CreationTimestamp
    @Column(name = "redeemed_at", nullable = false, updatable = false)
    private Date redeemedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class MilestoneRedemptionId implements Serializable {
        private PlayerSeason.PlayerSeasonId playerSeasonId;

        @Column(name = "streak_length")
        private Integer streakLength;
    }
}