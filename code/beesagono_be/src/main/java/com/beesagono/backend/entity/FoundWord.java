package com.beesagono.backend.entity;

import java.util.Date;

import com.beesagono.backend.entity.id.FoundWordId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

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
    private GameSession session;

    @CreationTimestamp
    @Column(name = "found_at", updatable = false, nullable = false)
    private Date foundAt;
}