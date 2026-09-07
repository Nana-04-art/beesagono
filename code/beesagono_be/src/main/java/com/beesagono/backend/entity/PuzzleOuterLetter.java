package com.beesagono.backend.entity;

import com.beesagono.backend.entity.id.PuzzleOuterLetterId;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "puzzle_outer_letters")
@SuperBuilder
public class PuzzleOuterLetter {

	@EmbeddedId
	@EqualsAndHashCode.Include
	@ToString.Include
	private PuzzleOuterLetterId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("puzzleId")
	@JoinColumn(name = "puzzle_id", nullable = false)
	private DailyPuzzle puzzle;
}