package com.taskman.user;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "app_user")
class AppUser {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	/** Stored in lower case, so lookups and the unique index agree. */
	@Column(nullable = false, length = 254)
	private String email;

	@Column(nullable = false, length = 100)
	private String displayName;

	@Column(nullable = false, length = 100)
	private String passwordHash;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected AppUser() {
		// for JPA
	}

	AppUser(String email, String displayName, String passwordHash) {
		this.email = email;
		this.displayName = displayName;
		this.passwordHash = passwordHash;
		this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
		this.updatedAt = this.createdAt;
	}

	UUID getId() {
		return id;
	}

	String getEmail() {
		return email;
	}

	String getDisplayName() {
		return displayName;
	}

	String getPasswordHash() {
		return passwordHash;
	}

}
