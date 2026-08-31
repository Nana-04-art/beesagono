package com.beesagono.entity;

import java.util.Date;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "game_sessions")
@SuperBuilder
public class GameSession {

    @Id
    @UuidGenerator
    private String id;

    @ManyToOne
    @JoinColumn(name = "puzzle_id", nullable = false)
    private DailyPuzzle puzzle;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "current_score", nullable = false)
    private Integer currentScore = 0;

    @Column(name = "current_rank_label", nullable = false, length = 50)
    private String currentRankLabel;

    @Builder.Default
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @CreationTimestamp
    @Column(name = "start_time", nullable = false, updatable = false)
    private Date startTime;

    @Column(name = "last_updated", nullable = false)
    private Date lastUpdated;

    @OneToMany(mappedBy = "gameSession")
    private List<FoundWord> foundWords;

    @OneToMany(mappedBy = "gameSession")
    private List<InvalidWordAttempt> invalidWordAttempts;
}