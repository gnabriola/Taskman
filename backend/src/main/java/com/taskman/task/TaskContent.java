package com.taskman.task;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.taskman.task.recurrence.Recurrence;

/**
 * The user-editable fields of a task, with the rules that tie them together.
 * Built from a request or from parsed quick-add text.
 */
record TaskContent(UUID projectId, String title, String description, LocalDate dueDate, LocalTime dueTime,
		int priority, Recurrence recurrence, Set<UUID> labelIds) {

	static final int DEFAULT_PRIORITY = 4;

	TaskContent {
		Objects.requireNonNull(projectId, "projectId");
		Objects.requireNonNull(title, "title");
		labelIds = (labelIds != null) ? Set.copyOf(labelIds) : Set.of();
		if (dueTime != null && dueDate == null) {
			throw new IllegalArgumentException("A due time needs a due date");
		}
		if (recurrence != null && dueDate == null) {
			throw new IllegalArgumentException("A recurring task needs a due date");
		}
		if (priority < 1 || priority > 4) {
			throw new IllegalArgumentException("Priority must be between 1 and 4");
		}
	}

}
