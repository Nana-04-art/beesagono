package com.beesagono.entity;

import java.util.Date;

import javax.lang.model.type.ErrorType;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invalid_word_attempts")
@SuperBuilder
public class InvalidWordAttempt {

    @Id
    @UuidGenerator
    private String id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession gameSession;

    @Column(name = "attempted_word", nullable = false, length = 100)
    private String attemptedWord;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_reason", nullable = false, length = 20)
    private ErrorType errorReason;

    @CreationTimestamp
    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Date attemptedAt;
}
