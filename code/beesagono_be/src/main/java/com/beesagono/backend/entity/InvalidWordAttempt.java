package com.beesagono.backend.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invalid_word_attempts")
@SuperBuilder
public class InvalidWordAttempt {

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @ManyToOne
    @JoinColumn(name = "error_reason", nullable = false)
    private ErrorType errorReason;

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "attempted_word", nullable = false, length = 100)
    private String attemptedWord;

    @CreationTimestamp
    @Column(name = "attempted_at", updatable = false, nullable = false)
    private Date attemptedAt;
}