package com.taskman.task;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * The user-editable fields of a task, with the rules that tie them together.
 * Built from a request or from parsed quick-add text.
 */
record TaskContent(UUID projectId, String title, String description, LocalDate dueDate, LocalTime dueTime,
		int priority) {

	static final int DEFAULT_PRIORITY = 4;

	TaskContent {
		Objects.requireNonNull(projectId, "projectId");
		Objects.requireNonNull(title, "title");
		if (dueTime != null && dueDate == null) {
			throw new IllegalArgumentException("A due time needs a due date");
		}
		if (priority < 1 || priority > 4) {
			throw new IllegalArgumentException("Priority must be between 1 and 4");
		}
	}

}
