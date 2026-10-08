package com.taskman.task;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Replaces all editable fields (PUT semantics): an omitted due date clears it, an
 * omitted priority resets it to 4.
 */
record UpdateTaskRequest(@NotBlank @Size(max = 500) String title, @Size(max = 10_000) String description,
		@NotNull UUID projectId, LocalDate dueDate, LocalTime dueTime, @Min(1) @Max(4) Integer priority) {

	UpdateTaskRequest(String title, String description, UUID projectId) {
		this(title, description, projectId, null, null, null);
	}

	@AssertTrue(message = "dueTime requires dueDate")
	boolean isDueTime() {
		return dueTime == null || dueDate != null;
	}

}
