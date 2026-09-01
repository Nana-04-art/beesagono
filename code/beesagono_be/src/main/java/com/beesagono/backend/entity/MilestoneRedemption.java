package com.beesagono.backend.entity;

import java.util.Date;
import com.beesagono.backend.entity.id.MilestoneRedemptionId;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "milestone_redemptions")
@SuperBuilder
public class MilestoneRedemption {

    @EmbeddedId
    private MilestoneRedemptionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false),
            @JoinColumn(name = "year", referencedColumnName = "year", insertable = false, updatable = false)
    })
    private PlayerSeason playerSeason;

    @CreationTimestamp
    @Column(name = "redeemed_at", updatable = false, nullable = false)
    private Date redeemedAt;
}