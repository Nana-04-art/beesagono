package com.beesagono.backend.entity;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "game_sessions", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "puzzle_id" }))
@SuperBuilder
public class GameSession {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puzzle_id", nullable = false)
    private DailyPuzzle puzzle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "session")
    private List<FoundWord> foundWords;

    @OneToMany(mappedBy = "session")
    private List<InvalidWordAttempt> invalidWordAttempts;

    @Id
    @UuidGenerator
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @Builder.Default
    @ToString.Include
    @Column(name = "current_score", nullable = false)
    private Integer currentScore = 0;

    /**
     * Denormalized cache of current_score/max_score resolved against
     * RANK_TIERS: must be written within the same transaction as
     * currentScore, never independently.
     */
    @ToString.Include
    @Column(name = "current_rank_label", nullable = false, length = 50)
    private String currentRankLabel;

    @Builder.Default
    @ToString.Include
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @CreationTimestamp
    @Column(name = "start_time", nullable = false, updatable = false)
    private Date startTime;

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private Date lastUpdated;
}