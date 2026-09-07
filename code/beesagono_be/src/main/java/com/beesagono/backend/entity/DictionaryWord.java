package com.beesagono.backend.entity;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "dictionary_words")
@SuperBuilder
public class DictionaryWord {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_user_id")
    private User addedByUser;

    @OneToMany(mappedBy = "dictionaryWord")
    private List<PuzzleWord> puzzleWords;

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "word", length = 100)
    private String word;

    @ToString.Include
    @Column(name = "word_length", insertable = false, updatable = false)
    private Integer wordLength;

    @Builder.Default
    @ToString.Include
    @Column(name = "unique_letters_count", nullable = false)
    private Integer uniqueLettersCount = 0;

    @Builder.Default
    @ToString.Include
    @Column(name = "is_candidate_pangram", nullable = false)
    private Boolean isCandidatePangram = false;

    @Builder.Default
    @ToString.Include
    @Column(name = "letter_mask", nullable = false)
    private Integer letterMask = 0;

    @CreationTimestamp
    @ToString.Include
    @Column(name = "added_at", updatable = false, nullable = false)
    private Date addedAt;
}