package com.taskman.task;

import java.time.Instant;
import java.util.UUID;

record TaskResponse(UUID id, UUID projectId, String title, String description, boolean completed,
		Instant completedAt, Instant createdAt, Instant updatedAt) {

	static TaskResponse from(Task task) {
		return new TaskResponse(task.getId(), task.getProjectId(), task.getTitle(), task.getDescription(),
				task.getCompletedAt() != null, task.getCompletedAt(), task.getCreatedAt(), task.getUpdatedAt());
	}

}
