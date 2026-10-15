package com.taskman.task;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.taskman.shared.validation.IanaTimeZone;
import com.taskman.shared.security.CurrentUser;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
	List<TaskResponse> findByProject(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID projectId,
			@RequestParam(required = false) Boolean completed) {
		return taskService.findByProject(user.id(), projectId, completed);
	}

	@PostMapping("/projects/{projectId}/tasks")
	ResponseEntity<TaskResponse> create(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID projectId,
			@Valid @RequestBody CreateTaskRequest request) {
		TaskResponse created = taskService.create(user.id(), projectId, request);
		return ResponseEntity.created(taskLocation(created)).body(created);
	}

	/** Open tasks matching a filter query, e.g. {@code ?filter=today | overdue&timeZone=Europe/Berlin}. */
	@GetMapping("/tasks")
	List<TaskResponse> findByFilter(@AuthenticationPrincipal CurrentUser user, @RequestParam String filter,
			@RequestParam(required = false) @IanaTimeZone String timeZone) {
		return taskService.findByFilter(user.id(), filter, timeZone);
	}

	@PostMapping("/tasks/quick-add")
	ResponseEntity<TaskResponse> quickAdd(@AuthenticationPrincipal CurrentUser user,
			@Valid @RequestBody QuickAddRequest request) {
		TaskResponse created = taskService.quickAdd(user.id(), request);
		return ResponseEntity.created(taskLocation(created)).body(created);
	}

	@GetMapping("/tasks/{id}")
	TaskResponse findById(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
		return taskService.findById(user.id(), id);
	}

	@PutMapping("/tasks/{id}")
	TaskResponse update(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
			@Valid @RequestBody UpdateTaskRequest request) {
		return taskService.update(user.id(), id, request);
	}

	@PostMapping("/tasks/{id}/complete")
	TaskResponse complete(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
			@Valid @RequestBody(required = false) CompleteTaskRequest request) {
		return taskService.complete(user.id(), id, (request != null) ? request.timeZone() : null);
	}

	@PostMapping("/tasks/{id}/reopen")
	TaskResponse reopen(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
		return taskService.reopen(user.id(), id);
	}

	@DeleteMapping("/tasks/{id}")
	ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
		taskService.delete(user.id(), id);
		return ResponseEntity.noContent().build();
	}

	private static URI taskLocation(TaskResponse task) {
		return ServletUriComponentsBuilder.fromCurrentContextPath()
			.path("/api/tasks/{id}")
			.buildAndExpand(task.id())
			.toUri();
	}

}
