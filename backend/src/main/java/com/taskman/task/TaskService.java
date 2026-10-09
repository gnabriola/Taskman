package com.taskman.task;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.taskman.label.LabelApi;
import com.taskman.project.ProjectApi;
import com.taskman.project.ProjectNotFoundException;
import com.taskman.task.filter.Filter;
import com.taskman.task.filter.FilterParser;
import com.taskman.task.quickadd.ParsedTask;
import com.taskman.task.quickadd.QuickAddParser;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TaskService {

	private final TaskRepository tasks;

	private final ProjectApi projects;

	private final LabelApi labels;

	private final Clock clock;

	private final QuickAddParser parser = new QuickAddParser();

	private final FilterParser filterParser = new FilterParser();

	/** Soonest first; undated last; all-day before timed; then by priority. */
	private static final Sort FILTER_ORDER = Sort.by(Sort.Order.asc("dueDate").nullsLast(),
			Sort.Order.asc("dueTime").nullsFirst(), Sort.Order.asc("priority"), Sort.Order.asc("createdAt"));

	TaskService(TaskRepository tasks, ProjectApi projects, LabelApi labels, Clock clock) {
		this.tasks = tasks;
		this.projects = projects;
		this.labels = labels;
		this.clock = clock;
	}

	/**
	 * @param completed {@code true} for completed tasks only, {@code false} for open
	 * tasks only, {@code null} for all tasks
	 */
	List<TaskResponse> findByProject(UUID projectId, Boolean completed) {
		requireProject(projectId);
		List<Task> found;
		if (completed == null) {
			found = tasks.findByProjectIdOrderByCreatedAtAsc(projectId);
		}
		else if (completed) {
			found = tasks.findByProjectIdAndCompletedAtIsNotNullOrderByCreatedAtAsc(projectId);
		}
		else {
			found = tasks.findByProjectIdAndCompletedAtIsNullOrderByCreatedAtAsc(projectId);
		}
		return found.stream().map(TaskResponse::from).toList();
	}

	/**
	 * Open tasks matching a filter query such as {@code (today | overdue) & #work}.
	 * @param timeZone the user's zone; decides what "today" and "overdue" mean
	 */
	List<TaskResponse> findByFilter(String query, String timeZone) {
		Filter filter = filterParser.parse(query);
		ZoneId zone = (timeZone != null) ? ZoneId.of(timeZone) : ZoneOffset.UTC;
		LocalDateTime now = LocalDateTime.now(clock.withZone(zone));
		var matching = TaskFilters.matching(filter, now,
				name -> projects.findIdByName(name).orElseThrow(() -> FilterRejectedException.unknownProject(name)),
				name -> labels.findIdByName(name).orElseThrow(() -> FilterRejectedException.unknownLabel(name)));
		return tasks.findAll(matching.and(TaskFilters.open()), FILTER_ORDER)
			.stream()
			.map(TaskResponse::from)
			.toList();
	}

	TaskResponse findById(UUID id) {
		return TaskResponse.from(load(id));
	}

	@Transactional
	TaskResponse create(UUID projectId, CreateTaskRequest request) {
		requireProject(projectId);
		labels.requireExisting(request.labelIdsOrEmpty());
		TaskContent content = new TaskContent(projectId, request.title().strip(), normalize(request.description()),
				request.dueDate(), request.dueTime(), priorityOrDefault(request.priority()), request.toRecurrence(),
				request.labelIdsOrEmpty());
		return TaskResponse.from(tasks.save(new Task(content)));
	}

	@Transactional
	TaskResponse quickAdd(QuickAddRequest request) {
		LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneId.of(request.timeZone())));
		ParsedTask parsed = parser.parse(request.text(), now);
		if (parsed.title().isBlank()) {
			throw QuickAddRejectedException.emptyTitle();
		}
		UUID projectId;
		if (parsed.projectName() != null) {
			projectId = projects.findIdByName(parsed.projectName())
				.orElseThrow(() -> QuickAddRejectedException.unknownProject(parsed.projectName()));
		}
		else {
			projectId = request.defaultProjectId();
			requireProject(projectId);
		}
		TaskContent content = new TaskContent(projectId, parsed.title(), null, parsed.dueDate(), parsed.dueTime(),
				priorityOrDefault(parsed.priority()), parsed.recurrence(),
				Set.copyOf(labels.findOrCreateByNames(parsed.labelNames())));
		return TaskResponse.from(tasks.save(new Task(content)));
	}

	@Transactional
	TaskResponse update(UUID id, UpdateTaskRequest request) {
		Task task = load(id);
		if (!task.getProjectId().equals(request.projectId())) {
			requireProject(request.projectId());
		}
		labels.requireExisting(request.labelIdsOrEmpty());
		task.update(new TaskContent(request.projectId(), request.title().strip(), normalize(request.description()),
				request.dueDate(), request.dueTime(), priorityOrDefault(request.priority()), request.toRecurrence(),
				request.labelIdsOrEmpty()));
		return TaskResponse.from(task);
	}

	@Transactional
	TaskResponse complete(UUID id, String timeZone) {
		Task task = load(id);
		ZoneId zone = (timeZone != null) ? ZoneId.of(timeZone) : ZoneOffset.UTC;
		task.complete(LocalDate.now(clock.withZone(zone)));
		return TaskResponse.from(task);
	}

	@Transactional
	TaskResponse reopen(UUID id) {
		Task task = load(id);
		task.reopen();
		return TaskResponse.from(task);
	}

	@Transactional
	void delete(UUID id) {
		tasks.delete(load(id));
	}

	private Task load(UUID id) {
		return tasks.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
	}

	private void requireProject(UUID projectId) {
		if (!projects.exists(projectId)) {
			throw new ProjectNotFoundException(projectId);
		}
	}

	private static int priorityOrDefault(Integer priority) {
		return (priority != null) ? priority : TaskContent.DEFAULT_PRIORITY;
	}

	private static String normalize(String description) {
		return (description == null || description.isBlank()) ? null : description.strip();
	}

}
