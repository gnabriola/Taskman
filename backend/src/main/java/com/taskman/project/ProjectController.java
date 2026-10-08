package com.taskman.project;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
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
	List<ProjectResponse> findAll() {
		return projectService.findAll();
	}

	@GetMapping("/{id}")
	ProjectResponse findById(@PathVariable UUID id) {
		return projectService.findById(id);
	}

	@PostMapping
	ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
		ProjectResponse created = projectService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	ProjectResponse rename(@PathVariable UUID id, @Valid @RequestBody ProjectRequest request) {
		return projectService.rename(id, request);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable UUID id) {
		projectService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
