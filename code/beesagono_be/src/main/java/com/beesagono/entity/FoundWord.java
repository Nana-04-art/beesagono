package com.beesagono.entity;

import java.io.Serializable;
import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "found_words")
@SuperBuilder
public class FoundWord {

    @EmbeddedId
    private FoundWordId id;

    @ManyToOne
    @MapsId("sessionId")
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession gameSession;

    @CreationTimestamp
    @Column(name = "found_at", nullable = false, updatable = false)
    private Date foundAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class FoundWordId implements Serializable {
        @Column(name = "session_id")
        private String sessionId;

        @Column(name = "word")
        private String word;
    }
}
