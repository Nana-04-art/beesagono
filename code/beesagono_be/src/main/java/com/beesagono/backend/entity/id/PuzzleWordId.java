package com.beesagono.backend.entity.id;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PuzzleWordId implements Serializable {

    @Column(name = "puzzle_id")
    private String puzzleId;

    @Column(name = "word")
    private String word;
}