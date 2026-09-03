package com.beesagono.backend.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "player_stats")
@SuperBuilder
public class PlayerStats {

    @Id
    @Column(name = "user_id")
    private String userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Builder.Default
    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;

    @Builder.Default
    @Column(name = "max_streak", nullable = false)
    private Integer maxStreak = 0;

    /**
     * Denormalized cache of the current season's total_points in
     * player_seasons: must remain synchronized within the same transaction.
     */
    @Builder.Default
    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Builder.Default
    @Column(name = "games_played", nullable = false)
    private Integer gamesPlayed = 0;

    @Builder.Default
    @Column(name = "games_completed", nullable = false)
    private Integer gamesCompleted = 0;

    @Column(name = "last_played_date")
    private LocalDate lastPlayedDate;
}