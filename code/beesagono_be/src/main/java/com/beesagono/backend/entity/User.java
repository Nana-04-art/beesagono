package com.beesagono.backend.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "users")
@SuperBuilder
public class User {

	@OneToMany(mappedBy = "user")
	private List<UserRole> userRoles;

	@OneToMany(mappedBy = "user")
	private List<RefreshToken> refreshTokens;

	@OneToMany(mappedBy = "addedByUser")
	private List<DictionaryWord> addedWords;

	@OneToMany(mappedBy = "user")
	private List<GameSession> gameSessions;

	@OneToMany(mappedBy = "user")
	private List<PlayerSeason> playerSeasons;

	@OneToMany(mappedBy = "user")
	private List<RankHistogram> rankHistogramEntries;

	@OneToOne(mappedBy = "user")
	private PlayerStats playerStats;

	@Id
	@UuidGenerator
	@EqualsAndHashCode.Include
	@ToString.Include
	private String id;

	@ToString.Include
	@Column(name = "username", nullable = false, unique = true, length = 50)
	private String username;

	@ToString.Include
	@Column(name = "email", nullable = false, unique = true, length = 150)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@CreationTimestamp
	@Column(name = "registered_at", updatable = false, nullable = false)
	private LocalDateTime registeredAt;
}