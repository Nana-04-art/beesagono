package com.beesagono.backend.entity;

import com.beesagono.backend.entity.id.PuzzleOuterLetterId;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "puzzle_outer_letters")
@SuperBuilder
public class PuzzleOuterLetter {

	@EmbeddedId
	private PuzzleOuterLetterId id;

	@ManyToOne
	@MapsId("puzzleId")
	@JoinColumn(name = "puzzle_id", nullable = false)
	private DailyPuzzle puzzle;
}