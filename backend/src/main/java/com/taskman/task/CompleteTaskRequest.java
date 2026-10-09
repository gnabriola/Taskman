package com.taskman.task;

import com.taskman.shared.validation.IanaTimeZone;

/**
 * Optional body of {@code POST /api/tasks/{id}/complete}.
 *
 * @param timeZone IANA zone of the user; decides what "today" is when a recurring
 * task moves to its next occurrence. Defaults to UTC.
 */
record CompleteTaskRequest(@IanaTimeZone String timeZone) {
}
