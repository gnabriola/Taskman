package com.taskman.label;

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
@RequestMapping("/api/labels")
class LabelController {

	private final LabelService labelService;

	LabelController(LabelService labelService) {
		this.labelService = labelService;
	}

	@GetMapping
	List<LabelResponse> findAll(@AuthenticationPrincipal CurrentUser user) {
		return labelService.findAll(user.id());
	}

	@PostMapping
	ResponseEntity<LabelResponse> create(@AuthenticationPrincipal CurrentUser user,
			@Valid @RequestBody LabelRequest request) {
		LabelResponse created = labelService.create(user.id(), request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	LabelResponse rename(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
			@Valid @RequestBody LabelRequest request) {
		return labelService.rename(user.id(), id, request);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
		labelService.delete(user.id(), id);
		return ResponseEntity.noContent().build();
	}

}
