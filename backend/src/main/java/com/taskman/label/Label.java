package com.taskman.label;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "label")
class Label {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, length = 60)
	private String name;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected Label() {
		// for JPA
	}

	Label(String name) {
		this.name = name;
		this.createdAt = now();
		this.updatedAt = this.createdAt;
	}

	void rename(String name) {
		this.name = name;
		this.updatedAt = now();
	}

	UUID getId() {
		return id;
	}

	String getName() {
		return name;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	// PostgreSQL stores microseconds; truncating keeps responses identical before and after a reload.
	private static Instant now() {
		return Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

}
