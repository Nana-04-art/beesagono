package com.beesagono.backend.entity;

import java.util.List;
import com.beesagono.backend.entity.id.PlayerSeasonId;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "player_seasons")
@SuperBuilder
public class PlayerSeason {

    @EmbeddedId
    @EqualsAndHashCode.Include
    @ToString.Include
    @AttributeOverride(name = "userId", column = @Column(name = "user_id"))
    private PlayerSeasonId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "playerSeason")
    private List<MilestoneRedemption> milestoneRedemptions;

    @Builder.Default
    @ToString.Include
    @Column(name = "base_points", nullable = false)
    private Integer basePoints = 0;

    @Builder.Default
    @ToString.Include
    @Column(name = "bonus_points", nullable = false)
    private Integer bonusPoints = 0;

    @Builder.Default
    @ToString.Include
    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @ToString.Include
    @Column(name = "highest_tier_achieved", length = 50)
    private String highestTierAchieved;
}