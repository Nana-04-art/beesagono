package com.beesagono.backend.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key for mapping badges unlocked or assigned to a specific
 * user
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class UserBadgeId implements Serializable {

    @Column(name = "user_id")
    private String userId;

    @Column(name = "badge_code")
    private String badgeCode;
}