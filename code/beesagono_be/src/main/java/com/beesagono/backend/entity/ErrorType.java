package com.beesagono.backend.entity;

import java.util.List;

import com.beesagono.backend.enums.ErrorTypeCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
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
@Table(name = "error_types")
@SuperBuilder
public class ErrorType {

    @OneToMany(mappedBy = "errorReason")
    private List<InvalidWordAttempt> invalidWordAttempts;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "code", length = 20)
    @EqualsAndHashCode.Include
    @ToString.Include
    private ErrorTypeCode code;

    @ToString.Include
    @Column(name = "description", nullable = false, length = 255)
    private String description;
}