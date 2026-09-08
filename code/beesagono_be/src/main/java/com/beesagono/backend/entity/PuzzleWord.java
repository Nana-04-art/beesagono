package com.beesagono.backend.entity;

import com.beesagono.backend.entity.id.PuzzleWordId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Table(name = "puzzle_words")
@SuperBuilder
public class PuzzleWord {

    @EmbeddedId
    @EqualsAndHashCode.Include
    @ToString.Include
    private PuzzleWordId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("puzzleId")
    @JoinColumn(name = "puzzle_id", nullable = false)
    private DailyPuzzle puzzle;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("word")
    @JoinColumn(name = "word", nullable = false)
    private DictionaryWord dictionaryWord;

    @Builder.Default
    @Column(name = "is_mielegramma", nullable = false)
    private Boolean isMielegramma = false;
}