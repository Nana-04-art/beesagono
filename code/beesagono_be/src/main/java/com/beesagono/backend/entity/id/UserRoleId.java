package com.beesagono.backend.entity.id;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key mapping system security roles to individual users.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserRoleId implements Serializable {

	@Column(name = "user_id")
	private String userId;

	@Column(name = "role_id")
	private String roleId;
}