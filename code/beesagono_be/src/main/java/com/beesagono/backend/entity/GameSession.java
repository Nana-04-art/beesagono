package com.beesagono.backend.entity;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "game_sessions", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "puzzle_id" }))
@SuperBuilder
public class GameSession {

    @ManyToOne
    @JoinColumn(name = "puzzle_id", nullable = false)
    private DailyPuzzle puzzle;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "session")
    private List<FoundWord> foundWords;

    @OneToMany(mappedBy = "session")
    private List<InvalidWordAttempt> invalidWordAttempts;

    @Id
    @UuidGenerator
    private String id;

    @Builder.Default
    @Column(name = "current_score", nullable = false)
    private Integer currentScore = 0;

    /**
     * Cache denormalizzata di current_score/max_score risolta contro
     * RANK_TIERS: deve essere scritta nella stessa transazione di
     * currentScore, mai in modo indipendente.
     */
    @Column(name = "current_rank_label", nullable = false, length = 50)
    private String currentRankLabel;

    @Builder.Default
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "start_time", nullable = false)
    private Date startTime;

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private Date lastUpdated;
}