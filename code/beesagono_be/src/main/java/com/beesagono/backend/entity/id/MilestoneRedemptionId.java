package com.beesagono.backend.entity.id;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class MilestoneRedemptionId implements Serializable {

    @Column(name = "user_id")
    private String userId;

    @Column(name = "year")
    private Integer year;

    @Column(name = "streak_length")
    private Integer streakLength;
}