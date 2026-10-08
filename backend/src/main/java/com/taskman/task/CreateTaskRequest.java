package com.taskman.task;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code priority} is 1 (highest) to 4; it defaults to 4 when omitted. */
record CreateTaskRequest(@NotBlank @Size(max = 500) String title, @Size(max = 10_000) String description,
		LocalDate dueDate, LocalTime dueTime, @Min(1) @Max(4) Integer priority) {

	CreateTaskRequest(String title, String description) {
		this(title, description, null, null, null);
	}

	@AssertTrue(message = "dueTime requires dueDate")
	boolean isDueTime() {
		return dueTime == null || dueDate != null;
	}

}
