package com.taskman.project;

import java.time.Instant;
import java.util.UUID;

record ProjectResponse(UUID id, String name, Instant createdAt, Instant updatedAt) {

	static ProjectResponse from(Project project) {
		return new ProjectResponse(project.getId(), project.getName(), project.getCreatedAt(),
				project.getUpdatedAt());
	}

}
