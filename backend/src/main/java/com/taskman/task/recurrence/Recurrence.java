package com.taskman.task.recurrence;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Set;

/**
 * How a task repeats. Stored as a subset of an RFC 5545 RRULE, e.g.
 * {@code FREQ=WEEKLY;BYDAY=MO,WE,FR}, plus a flag for Todoist's "every!" (count from
 * the completion date instead of the due date).
 */
public record Recurrence(Frequency frequency, int interval, Set<DayOfWeek> days, Integer monthDay, Month month,
		boolean fromCompletion) {

	public static Recurrence parse(String rule, boolean fromCompletion) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public String rule() {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public LocalDate nextAfter(LocalDate date) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public LocalDate firstOnOrAfter(LocalDate date) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public LocalDate nextDue(LocalDate dueDate, LocalDate today) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public String label() {
		throw new UnsupportedOperationException("Not implemented yet");
	}

}
