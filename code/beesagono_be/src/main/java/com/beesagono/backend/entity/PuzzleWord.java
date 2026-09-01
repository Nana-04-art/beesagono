package com.beesagono.backend.entity;

import com.beesagono.backend.entity.id.PuzzleWordId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
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
@Table(name = "puzzle_words")
@SuperBuilder
public class PuzzleWord {

    @EmbeddedId
    private PuzzleWordId id;

    @ManyToOne
    @MapsId("puzzleId")
    @JoinColumn(name = "puzzle_id", nullable = false)
    private DailyPuzzle puzzle;

    @ManyToOne
    @MapsId("word")
    @JoinColumn(name = "word", nullable = false)
    private DictionaryWord dictionaryWord;

    @Builder.Default
    @Column(name = "is_mielegramma", nullable = false)
    private Boolean isMielegramma = false;
}