package com.taskman.task;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.taskman.task.recurrence.Recurrence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "task")
class Task {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	// Same as the project's owner; stored here so every task query can be scoped by owner.
	@Column(nullable = false, updatable = false)
	private UUID ownerId;

	// A plain id, not a @ManyToOne Project: the project entity belongs to another module.
	@Column(nullable = false)
	private UUID projectId;

	@Column(nullable = false, length = 500)
	private String title;

	private String description;

	private LocalDate dueDate;

	private LocalTime dueTime;

	@Column(nullable = false)
	private int priority;

	// Recurrence is a value object spread over two columns; see getRecurrence().
	private String recurrence;

	@Column(nullable = false)
	private boolean recurrenceFromCompletion;

	// Plain label ids, like projectId: the label entity belongs to another module.
	// BatchSize loads the labels of many tasks in one query instead of one per task.
	@ElementCollection
	@CollectionTable(name = "task_label", joinColumns = @JoinColumn(name = "task_id"))
	@Column(name = "label_id")
	@BatchSize(size = 50)
	private Set<UUID> labelIds = new HashSet<>();

	private Instant completedAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected Task() {
		// for JPA
	}

	Task(UUID ownerId, TaskContent content) {
		this.ownerId = ownerId;
		apply(content);
		this.createdAt = now();
		this.updatedAt = this.createdAt;
	}

	void update(TaskContent content) {
		apply(content);
		this.updatedAt = now();
	}

	private void apply(TaskContent content) {
		this.projectId = content.projectId();
		this.title = content.title();
		this.description = content.description();
		this.dueDate = content.dueDate();
		this.dueTime = content.dueTime();
		this.priority = content.priority();
		this.recurrence = (content.recurrence() != null) ? content.recurrence().rule() : null;
		this.recurrenceFromCompletion = content.recurrence() != null && content.recurrence().fromCompletion();
		this.labelIds.clear();
		this.labelIds.addAll(content.labelIds());
	}

	/**
	 * Completes the task. A recurring task stays open and moves to its next
	 * occurrence instead. Completing an already completed task keeps the original
	 * completion time.
	 * @param today the user's current date, needed to skip occurrences already past
	 */
	void complete(LocalDate today) {
		Recurrence rule = getRecurrence();
		if (rule != null) {
			dueDate = rule.nextDue(dueDate, today);
			updatedAt = now();
		}
		else if (completedAt == null) {
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

	LocalDate getDueDate() {
		return dueDate;
	}

	LocalTime getDueTime() {
		return dueTime;
	}

	int getPriority() {
		return priority;
	}

	Recurrence getRecurrence() {
		return (recurrence != null) ? Recurrence.parse(recurrence, recurrenceFromCompletion) : null;
	}

	/** Sorted, so responses list labels in a stable order. */
	List<UUID> getLabelIds() {
		return labelIds.stream().sorted().toList();
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
