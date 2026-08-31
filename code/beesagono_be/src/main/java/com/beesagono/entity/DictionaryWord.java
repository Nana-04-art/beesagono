package com.beesagono.entity;

import java.util.Date;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "dictionary_words")
@SuperBuilder
public class DictionaryWord {

    @Id
    @Column(name = "word", length = 100)
    private String word;

    @Column(name = "word_length", nullable = false)
    private Integer wordLength;

    @Column(name = "unique_letters_count", nullable = false)
    private Integer uniqueLettersCount;

    @Builder.Default
    @Column(name = "is_candidate_pangram", nullable = false)
    private Boolean isCandidatePangram = false;

    @ManyToOne
    @JoinColumn(name = "added_by_user_id")
    private User addedByUser;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private Date addedAt;

    @OneToMany(mappedBy = "word")
    private List<PuzzleWord> puzzleWords;
}