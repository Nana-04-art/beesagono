package com.beesagono.backend.entity.id;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key mapping allowable outer letters to a specific daily puzzle instance.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PuzzleOuterLetterId implements Serializable {

    @Column(name = "puzzle_id")
    private String puzzleId;

    @Column(name = "letter")
    private String letter;
}