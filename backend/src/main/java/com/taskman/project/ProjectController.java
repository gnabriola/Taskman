package com.taskman.project;

import java.net.URI;
import java.util.List;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/projects")
class ProjectController {

	private final ProjectService projectService;

	ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping
	List<ProjectResponse> findAll(@AuthenticationPrincipal CurrentUser user) {
		return projectService.findAll(user.id());
	}

	@GetMapping("/{id}")
	ProjectResponse findById(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
		return projectService.findById(user.id(), id);
	}

	@PostMapping
	ResponseEntity<ProjectResponse> create(@AuthenticationPrincipal CurrentUser user,
			@Valid @RequestBody ProjectRequest request) {
		ProjectResponse created = projectService.create(user.id(), request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	ProjectResponse rename(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
			@Valid @RequestBody ProjectRequest request) {
		return projectService.rename(user.id(), id, request);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
		projectService.delete(user.id(), id);
		return ResponseEntity.noContent().build();
	}

}
