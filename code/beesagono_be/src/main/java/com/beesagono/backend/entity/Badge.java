package com.beesagono.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity representing a catalog badge/achievement available in the game.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "badges")
@Builder
public class Badge {

    @Id
    @Column(name = "code", length = 50)
    @EqualsAndHashCode.Include
    private String code; // e.g. "FIRST_WORD", "FIRST_PANGRAM", "STREAK_7"

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "icon_url", length = 255)
    private String iconUrl;

    @Column(name = "category", length = 50)
    private String category; // e.g. "STREAK", "SCORE", "SPECIAL"
}