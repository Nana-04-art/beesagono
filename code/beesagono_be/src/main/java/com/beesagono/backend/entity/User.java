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
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Data
@NoArgsConstructor
@AllArgsConstructor
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

	/**
	 * Shared-key one-to-one relationship with player_stats. 
	 * Note: in the SQL schema, the FK is physically declared on users.id -> player_stats.user_id
	 * (direction reversed compared to the norm); here it is modeled in the
	 * conventional way, with PlayerStats owning the FK via @MapsId.
	 */
	@OneToOne(mappedBy = "user")
	private PlayerStats playerStats;

	@Id
	@UuidGenerator
	private String id;

	@Column(name = "username", nullable = false, unique = true, length = 50)
	private String username;

	@Column(name = "email", nullable = false, unique = true, length = 150)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@CreationTimestamp
	@Column(name = "registered_at", updatable = false, nullable = false)
	private LocalDateTime registeredAt;
}