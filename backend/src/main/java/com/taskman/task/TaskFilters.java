package com.taskman.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.function.Function;

import com.taskman.task.filter.Filter;

import org.springframework.data.jpa.domain.Specification;

/**
 * Turns a parsed {@link Filter} into a JPA {@link Specification}, so the database does
 * the filtering.
 * <p>
 * Every leaf condition is written to be {@code TRUE} or {@code FALSE}, never
 * {@code NULL}. SQL uses three-valued logic: for a task without a due date,
 * {@code due_date = ?} is {@code NULL}, and {@code NOT NULL} is still {@code NULL},
 * so a naive {@code !today} would silently drop undated tasks. Guarding with
 * {@code due_date IS NOT NULL AND ...} makes it {@code FALSE}, which {@code NOT} turns
 * into {@code TRUE} as expected.
 */
final class TaskFilters {

	private TaskFilters() {
	}

	/**
	 * @param now the user's current wall-clock date and time
	 * @param projectId resolves a project name to its id (or throws)
	 * @param labelId resolves a label name to its id (or throws)
	 */
	static Specification<Task> matching(Filter filter, LocalDateTime now, Function<String, UUID> projectId,
			Function<String, UUID> labelId) {
		// Exhaustive: Filter is sealed, so a new node type fails to compile here until handled.
		return switch (filter) {
			case Filter.And(Filter left, Filter right) ->
				matching(left, now, projectId, labelId).and(matching(right, now, projectId, labelId));
			case Filter.Or(Filter left, Filter right) ->
				matching(left, now, projectId, labelId).or(matching(right, now, projectId, labelId));
			case Filter.Not(Filter inner) -> Specification.not(matching(inner, now, projectId, labelId));
			case Filter.Today today -> dueOn(now.toLocalDate());
			case Filter.Tomorrow tomorrow -> dueOn(now.toLocalDate().plusDays(1));
			case Filter.Overdue overdue -> overdue(now.toLocalDate(), now.toLocalTime());
			case Filter.NoDate noDate -> (root, query, cb) -> cb.isNull(root.get("dueDate"));
			case Filter.Recurring recurring -> (root, query, cb) -> cb.isNotNull(root.get("recurrence"));
			case Filter.Priority(int level) -> (root, query, cb) -> cb.equal(root.get("priority"), level);
			case Filter.Project(String name) -> {
				// Resolve now, while building, so an unknown name fails before any query runs.
				UUID id = projectId.apply(name);
				yield (root, query, cb) -> cb.equal(root.get("projectId"), id);
			}
			case Filter.Label(String name) -> {
				UUID id = labelId.apply(name);
				yield (root, query, cb) -> cb.isMember(id, root.get("labelIds"));
			}
		};
	}

	static Specification<Task> ownedBy(UUID ownerId) {
		return (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
	}

	static Specification<Task> open() {
		return (root, query, cb) -> cb.isNull(root.get("completedAt"));
	}

	private static Specification<Task> dueOn(LocalDate date) {
		return (root, query, cb) -> cb.and(cb.isNotNull(root.get("dueDate")), cb.equal(root.get("dueDate"), date));
	}

	/** Due before today, or today at a time that has already passed. */
	private static Specification<Task> overdue(LocalDate today, LocalTime now) {
		return (root, query, cb) -> cb.and(cb.isNotNull(root.get("dueDate")),
				cb.or(cb.lessThan(root.<LocalDate>get("dueDate"), today),
						cb.and(cb.equal(root.get("dueDate"), today), cb.isNotNull(root.get("dueTime")),
								cb.lessThan(root.<LocalTime>get("dueTime"), now))));
	}

}
