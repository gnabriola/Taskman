package com.taskman.task.quickadd;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Result of parsing quick-add text. Everything the parser did not recognise ends
 * up in {@code title}, which may be empty; deciding whether that is acceptable is
 * up to the caller.
 */
public record ParsedTask(String title, LocalDate dueDate, LocalTime dueTime, String projectName,
		Integer priority) {
}
