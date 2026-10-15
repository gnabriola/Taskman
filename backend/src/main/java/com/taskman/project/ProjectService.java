package com.taskman.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ProjectService implements ProjectApi {

	private final ProjectRepository projects;

	private final ApplicationEventPublisher events;

	ProjectService(ProjectRepository projects, ApplicationEventPublisher events) {
		this.projects = projects;
		this.events = events;
	}

	@Override
	public boolean exists(UUID ownerId, UUID projectId) {
		return projects.existsByIdAndOwnerId(projectId, ownerId);
	}

	@Override
	public Optional<UUID> findIdByName(UUID ownerId, String name) {
		return projects.findFirstByOwnerIdAndNameIgnoreCaseOrderByCreatedAtAsc(ownerId, name).map(Project::getId);
	}

	List<ProjectResponse> findAll(UUID ownerId) {
		return projects.findAllByOwnerIdOrderByCreatedAtAsc(ownerId).stream().map(ProjectResponse::from).toList();
	}

	ProjectResponse findById(UUID ownerId, UUID id) {
		return ProjectResponse.from(load(ownerId, id));
	}

	@Transactional
	ProjectResponse create(UUID ownerId, ProjectRequest request) {
		return ProjectResponse.from(projects.save(new Project(ownerId, request.name().strip())));
	}

	@Transactional
	ProjectResponse rename(UUID ownerId, UUID id, ProjectRequest request) {
		Project project = load(ownerId, id);
		project.rename(request.name().strip());
		return ProjectResponse.from(project);
	}

	@Transactional
	void delete(UUID ownerId, UUID id) {
		Project project = load(ownerId, id);
		// Listeners run synchronously in this transaction, so their cleanup and this
		// delete commit or roll back together.
		events.publishEvent(new ProjectDeletedEvent(id));
		projects.delete(project);
	}

	/**
	 * Another user's project is reported as not found (404), not forbidden (403): a 403
	 * would confirm that the id exists.
	 */
	private Project load(UUID ownerId, UUID id) {
		return projects.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new ProjectNotFoundException(id));
	}

}
