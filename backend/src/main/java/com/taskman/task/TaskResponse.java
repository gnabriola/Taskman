package com.taskman.task;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.taskman.task.recurrence.Recurrence;

record TaskResponse(UUID id, UUID projectId, String title, String description, LocalDate dueDate,
		LocalTime dueTime, int priority, String recurrence, boolean recurrenceFromCompletion,
		String recurrenceLabel, List<UUID> labelIds, boolean completed, Instant completedAt, Instant createdAt, Instant updatedAt) {

	static TaskResponse from(Task task) {
		Recurrence recurrence = task.getRecurrence();
		return new TaskResponse(task.getId(), task.getProjectId(), task.getTitle(), task.getDescription(),
				task.getDueDate(), task.getDueTime(), task.getPriority(),
				(recurrence != null) ? recurrence.rule() : null, recurrence != null && recurrence.fromCompletion(),
				(recurrence != null) ? recurrence.label() : null, task.getLabelIds(), task.getCompletedAt() != null,
				task.getCompletedAt(), task.getCreatedAt(), task.getUpdatedAt());
	}

}
