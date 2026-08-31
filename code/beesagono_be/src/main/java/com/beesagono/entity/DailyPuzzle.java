package com.beesagono.entity;

import java.util.Date;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_puzzles")
@SuperBuilder
public class DailyPuzzle {

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "puzzle_date", nullable = false, unique = true)
    private Date puzzleDate;

    @Column(name = "center_letter", nullable = false, length = 1)
    private String centerLetter;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @Column(name = "seed", length = 50)
    private String seed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @ElementCollection
    @CollectionTable(name = "puzzle_outer_letters", joinColumns = @JoinColumn(name = "puzzle_id"))
    @Column(name = "letter", nullable = false, length = 1)
    private List<String> outerLetters;

    @OneToMany(mappedBy = "puzzle")
    private List<PuzzleWord> puzzleWords;

    @OneToMany(mappedBy = "puzzle")
    private List<GameSession> gameSessions;
}
