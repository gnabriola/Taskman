package com.taskman.task;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

record TaskResponse(UUID id, UUID projectId, String title, String description, LocalDate dueDate,
		LocalTime dueTime, int priority, boolean completed, Instant completedAt, Instant createdAt,
		Instant updatedAt) {

	static TaskResponse from(Task task) {
		return new TaskResponse(task.getId(), task.getProjectId(), task.getTitle(), task.getDescription(),
				task.getDueDate(), task.getDueTime(), task.getPriority(), task.getCompletedAt() != null,
				task.getCompletedAt(), task.getCreatedAt(), task.getUpdatedAt());
	}

}
