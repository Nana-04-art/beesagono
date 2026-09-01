package com.beesagono.backend.entity;

import java.util.Date;

import com.beesagono.backend.entity.id.MilestoneRedemptionId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    @CreationTimestamp
    @Column(name = "redeemed_at", updatable = false, nullable = false)
    private Date redeemedAt;
}