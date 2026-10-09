package com.beesagono.backend.entity;

import com.beesagono.backend.entity.id.UserBadgeId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Entity representing a badge unlocked by a specific user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "user_badges")
@Builder
public class UserBadge {

    @EmbeddedId
    @EqualsAndHashCode.Include
    @ToString.Include
    private UserBadgeId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("badgeCode")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "badge_code", nullable = false)
    private Badge badge;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt; // Date of the game event (e.g., first word found)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // Record creation timestamp in DB
}