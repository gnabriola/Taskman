package com.taskman.task;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import com.taskman.task.recurrence.Recurrence;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Replaces all editable fields (PUT semantics): an omitted due date or recurrence
 * clears it, an omitted priority resets it to 4, omitted labelIds remove all labels.
 */
record UpdateTaskRequest(@NotBlank @Size(max = 500) String title, @Size(max = 10_000) String description,
		@NotNull UUID projectId, LocalDate dueDate, LocalTime dueTime, @Min(1) @Max(4) Integer priority,
		@Size(max = 200) String recurrence, Boolean recurrenceFromCompletion, @Size(max = 20) Set<UUID> labelIds) {

	UpdateTaskRequest(String title, String description, UUID projectId) {
		this(title, description, projectId, null, null, null, null, null, null);
	}

	@AssertTrue(message = "dueTime requires dueDate")
	boolean isDueTime() {
		return dueTime == null || dueDate != null;
	}

	@AssertTrue(message = "must be a supported RRULE, e.g. FREQ=WEEKLY;BYDAY=MO")
	boolean isRecurrence() {
		return recurrence == null || Recurrence.isValid(recurrence);
	}

	@AssertTrue(message = "is required for a recurring task")
	boolean isDueDate() {
		return recurrence == null || dueDate != null;
	}

	Set<UUID> labelIdsOrEmpty() {
		return (labelIds != null) ? labelIds : Set.of();
	}

	Recurrence toRecurrence() {
		return (recurrence != null) ? Recurrence.parse(recurrence, Boolean.TRUE.equals(recurrenceFromCompletion))
				: null;
	}

}
