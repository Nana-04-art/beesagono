package com.beesagono.entity;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "player_seasons")
@SuperBuilder
public class PlayerSeason {

    @EmbeddedId
    private PlayerSeasonId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "base_points", nullable = false)
    private Integer basePoints = 0;

    @Builder.Default
    @Column(name = "bonus_points", nullable = false)
    private Integer bonusPoints = 0;

    @Builder.Default
    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Column(name = "highest_tier_achieved", length = 50)
    private String highestTierAchieved;

    @OneToMany(mappedBy = "playerSeason")
    private List<MilestoneRedemption> milestoneRedemptions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class PlayerSeasonId implements Serializable {
        @Column(name = "user_id")
        private String userId;

        @Column(name = "year")
        private Integer year;
    }
}