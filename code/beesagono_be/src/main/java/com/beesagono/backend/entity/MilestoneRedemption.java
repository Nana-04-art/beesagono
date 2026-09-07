package com.beesagono.backend.entity;

import java.util.Date;

import com.beesagono.backend.entity.id.MilestoneRedemptionId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "milestone_redemptions")
@SuperBuilder
public class MilestoneRedemption {

    @EmbeddedId
    @EqualsAndHashCode.Include
    @ToString.Include
    private MilestoneRedemptionId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false),
            @JoinColumn(name = "season_year", referencedColumnName = "season_year", insertable = false, updatable = false)
    })
    private PlayerSeason playerSeason;

    @CreationTimestamp
    @ToString.Include
    @Column(name = "redeemed_at", updatable = false, nullable = false)
    private Date redeemedAt;
}