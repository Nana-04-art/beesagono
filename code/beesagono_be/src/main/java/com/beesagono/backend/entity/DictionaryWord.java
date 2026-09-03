package com.beesagono.backend.entity;

import java.util.Date;
import java.util.List;

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
import org.hibernate.annotations.CreationTimestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dictionary_words")
@SuperBuilder
public class DictionaryWord {

    @ManyToOne
    @JoinColumn(name = "added_by_user_id")
    private User addedByUser;

    @OneToMany(mappedBy = "dictionaryWord")
    private List<PuzzleWord> puzzleWords;

    @Id
    @Column(name = "word", length = 100)
    private String word;

    @Column(name = "word_length", insertable = false, updatable = false)
    private Integer wordLength;

    @Builder.Default
    @Column(name = "unique_letters_count", nullable = false)
    private Integer uniqueLettersCount = 0;

    @Builder.Default
    @Column(name = "is_candidate_pangram", nullable = false)
    private Boolean isCandidatePangram = false;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false, nullable = false)
    private Date addedAt;
}