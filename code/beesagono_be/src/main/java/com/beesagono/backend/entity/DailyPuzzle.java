package com.beesagono.backend.entity;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_puzzles")
@SuperBuilder
public class DailyPuzzle {

	@OneToMany(mappedBy = "puzzle")
	private List<PuzzleOuterLetter> outerLetters;

	@OneToMany(mappedBy = "puzzle")
	private List<PuzzleWord> puzzleWords;

	@OneToMany(mappedBy = "puzzle")
	private List<GameSession> gameSessions;

	@Id
	@UuidGenerator
	private String id;

	@Column(name = "puzzle_date", nullable = false, unique = true)
	private LocalDate puzzleDate;

	@Column(name = "center_letter", nullable = false, length = 1)
	private String centerLetter;

	@Column(name = "max_score", nullable = false)
	private Integer maxScore;

	@Column(name = "seed", nullable = false, length = 50)
	private String seed;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false, nullable = false)
	private Date createdAt;
}