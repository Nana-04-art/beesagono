package com.beesagono.backend.entity;

import java.util.Date;

import com.beesagono.backend.entity.id.FoundWordId;

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
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "found_words")
@SuperBuilder
public class FoundWord {

    @EmbeddedId
    @EqualsAndHashCode.Include
    @ToString.Include
    private FoundWordId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sessionId")
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @Column(name = "score_assigned", nullable = false)
    private Integer scoreAssigned;

    @Builder.Default
    @Column(name = "is_mielegramma", nullable = false)
    private Boolean isMielegramma = false;

    @CreationTimestamp
    @ToString.Include
    @Column(name = "found_at", updatable = false, nullable = false)
    private Date foundAt;
}