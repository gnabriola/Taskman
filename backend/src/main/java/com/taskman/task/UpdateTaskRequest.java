package com.taskman.task;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record UpdateTaskRequest(@NotBlank @Size(max = 500) String title, @Size(max = 10_000) String description,
		@NotNull UUID projectId) {
}
