package com.taskman.task;

import java.util.List;
import java.util.UUID;

import com.taskman.project.ProjectApi;
import com.taskman.project.ProjectNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TaskService {

	private final TaskRepository tasks;

	private final ProjectApi projects;

	TaskService(TaskRepository tasks, ProjectApi projects) {
		this.tasks = tasks;
		this.projects = projects;
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

	TaskResponse findById(UUID id) {
		return TaskResponse.from(load(id));
	}

	@Transactional
	TaskResponse create(UUID projectId, CreateTaskRequest request) {
		requireProject(projectId);
		TaskContent content = new TaskContent(projectId, request.title().strip(), normalize(request.description()),
				request.dueDate(), request.dueTime(), priorityOrDefault(request.priority()));
		return TaskResponse.from(tasks.save(new Task(content)));
	}

	@Transactional
	TaskResponse update(UUID id, UpdateTaskRequest request) {
		Task task = load(id);
		if (!task.getProjectId().equals(request.projectId())) {
			requireProject(request.projectId());
		}
		task.update(new TaskContent(request.projectId(), request.title().strip(), normalize(request.description()),
				request.dueDate(), request.dueTime(), priorityOrDefault(request.priority())));
		return TaskResponse.from(task);
	}

	@Transactional
	TaskResponse complete(UUID id) {
		Task task = load(id);
		task.complete();
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
