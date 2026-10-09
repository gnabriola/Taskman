package com.taskman.label;

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
@RequestMapping("/api/labels")
class LabelController {

	private final LabelService labelService;

	LabelController(LabelService labelService) {
		this.labelService = labelService;
	}

	@GetMapping
	List<LabelResponse> findAll() {
		return labelService.findAll();
	}

	@PostMapping
	ResponseEntity<LabelResponse> create(@Valid @RequestBody LabelRequest request) {
		LabelResponse created = labelService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	LabelResponse rename(@PathVariable UUID id, @Valid @RequestBody LabelRequest request) {
		return labelService.rename(id, request);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable UUID id) {
		labelService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
