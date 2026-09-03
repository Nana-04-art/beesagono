package com.beesagono.backend.entity;

import java.util.List;
import com.beesagono.backend.entity.id.PlayerSeasonId;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "player_seasons")
@SuperBuilder
public class PlayerSeason {

    @EmbeddedId
    @AttributeOverride(name = "userId", column = @Column(name = "user_id"))
    private PlayerSeasonId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "playerSeason")
    private List<MilestoneRedemption> milestoneRedemptions;

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
}