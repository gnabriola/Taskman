package com.taskman.task.quickadd;

import java.time.LocalDate;
import java.time.LocalTime;

import com.taskman.task.recurrence.Recurrence;

/**
 * Result of parsing quick-add text. Everything the parser did not recognise ends
 * up in {@code title}, which may be empty; deciding whether that is acceptable is
 * up to the caller. A recurring task has a {@code recurrence} and its first
 * occurrence as {@code dueDate}.
 */
public record ParsedTask(String title, LocalDate dueDate, LocalTime dueTime, String projectName, Integer priority,
		Recurrence recurrence) {
}
