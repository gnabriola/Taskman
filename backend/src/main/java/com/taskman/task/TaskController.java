package com.taskman.task;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.taskman.shared.validation.IanaTimeZone;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
class TaskController {

	private final TaskService taskService;

	TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@GetMapping("/projects/{projectId}/tasks")
	List<TaskResponse> findByProject(@PathVariable UUID projectId,
			@RequestParam(required = false) Boolean completed) {
		return taskService.findByProject(projectId, completed);
	}

	@PostMapping("/projects/{projectId}/tasks")
	ResponseEntity<TaskResponse> create(@PathVariable UUID projectId, @Valid @RequestBody CreateTaskRequest request) {
		TaskResponse created = taskService.create(projectId, request);
		return ResponseEntity.created(taskLocation(created)).body(created);
	}

	/** Open tasks matching a filter query, e.g. {@code ?filter=today | overdue&timeZone=Europe/Berlin}. */
	@GetMapping("/tasks")
	List<TaskResponse> findByFilter(@RequestParam String filter,
			@RequestParam(required = false) @IanaTimeZone String timeZone) {
		return taskService.findByFilter(filter, timeZone);
	}

	@PostMapping("/tasks/quick-add")
	ResponseEntity<TaskResponse> quickAdd(@Valid @RequestBody QuickAddRequest request) {
		TaskResponse created = taskService.quickAdd(request);
		return ResponseEntity.created(taskLocation(created)).body(created);
	}

	@GetMapping("/tasks/{id}")
	TaskResponse findById(@PathVariable UUID id) {
		return taskService.findById(id);
	}

	@PutMapping("/tasks/{id}")
	TaskResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
		return taskService.update(id, request);
	}

	@PostMapping("/tasks/{id}/complete")
	TaskResponse complete(@PathVariable UUID id, @Valid @RequestBody(required = false) CompleteTaskRequest request) {
		return taskService.complete(id, (request != null) ? request.timeZone() : null);
	}

	@PostMapping("/tasks/{id}/reopen")
	TaskResponse reopen(@PathVariable UUID id) {
		return taskService.reopen(id);
	}

	@DeleteMapping("/tasks/{id}")
	ResponseEntity<Void> delete(@PathVariable UUID id) {
		taskService.delete(id);
		return ResponseEntity.noContent().build();
	}

	private static URI taskLocation(TaskResponse task) {
		return ServletUriComponentsBuilder.fromCurrentContextPath()
			.path("/api/tasks/{id}")
			.buildAndExpand(task.id())
			.toUri();
	}

}
