package com.taskman.task;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param text the quick-add text, e.g. "pay rent tomorrow 5pm #home p1"
 * @param defaultProjectId used when the text names no {@code #project}
 * @param timeZone IANA zone of the user, e.g. "Europe/Berlin"; it decides what
 * "today" and "5pm" mean
 */
record QuickAddRequest(@NotBlank @Size(max = 500) String text, @NotNull UUID defaultProjectId,
		@NotBlank String timeZone) {

	@AssertTrue(message = "must be a valid IANA time zone, e.g. Europe/Berlin")
	boolean isTimeZone() {
		if (timeZone == null || timeZone.isBlank()) {
			return true; // reported by @NotBlank
		}
		try {
			ZoneId.of(timeZone);
			return true;
		}
		catch (DateTimeException ex) {
			return false;
		}
	}

}
