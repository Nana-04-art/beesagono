package com.beesagono.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
    private DictionaryWord word;

    @Builder.Default
    @Column(name = "is_mielegramma", nullable = false)
    private Boolean isMielegramma = false;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class PuzzleWordId implements Serializable {
        @Column(name = "puzzle_id")
        private String puzzleId;

        @Column(name = "word")
        private String word;
    }
}