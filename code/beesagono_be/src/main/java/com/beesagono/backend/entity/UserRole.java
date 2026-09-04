package com.beesagono.backend.entity;


import com.beesagono.backend.entity.id.UserRoleId;

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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_roles")
@SuperBuilder
public class UserRole {

	@EmbeddedId
	private UserRoleId id;

	@ManyToOne
	@MapsId("userId")
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne
	@MapsId("roleId")
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;
}