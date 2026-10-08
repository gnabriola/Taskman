package com.taskman.task;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "task")
class Task {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	// A plain id, not a @ManyToOne Project: the project entity belongs to another module.
	@Column(nullable = false)
	private UUID projectId;

	@Column(nullable = false, length = 500)
	private String title;

	private String description;

	private Instant completedAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected Task() {
		// for JPA
	}

	Task(UUID projectId, String title, String description) {
		this.projectId = projectId;
		this.title = title;
		this.description = description;
		this.createdAt = now();
		this.updatedAt = this.createdAt;
	}

	void update(UUID projectId, String title, String description) {
		this.projectId = projectId;
		this.title = title;
		this.description = description;
		this.updatedAt = now();
	}

	/** Completing an already completed task keeps the original completion time. */
	void complete() {
		if (completedAt == null) {
			completedAt = now();
			updatedAt = completedAt;
		}
	}

	void reopen() {
		if (completedAt != null) {
			completedAt = null;
			updatedAt = now();
		}
	}

	UUID getId() {
		return id;
	}

	UUID getProjectId() {
		return projectId;
	}

	String getTitle() {
		return title;
	}

	String getDescription() {
		return description;
	}

	Instant getCompletedAt() {
		return completedAt;
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
