package com.taskman.project;

import java.util.List;
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
	public boolean exists(UUID projectId) {
		return projects.existsById(projectId);
	}

	List<ProjectResponse> findAll() {
		return projects.findAllByOrderByCreatedAtAsc().stream().map(ProjectResponse::from).toList();
	}

	ProjectResponse findById(UUID id) {
		return ProjectResponse.from(load(id));
	}

	@Transactional
	ProjectResponse create(ProjectRequest request) {
		return ProjectResponse.from(projects.save(new Project(request.name().strip())));
	}

	@Transactional
	ProjectResponse rename(UUID id, ProjectRequest request) {
		Project project = load(id);
		project.rename(request.name().strip());
		return ProjectResponse.from(project);
	}

	@Transactional
	void delete(UUID id) {
		Project project = load(id);
		// Listeners run synchronously in this transaction, so their cleanup and this
		// delete commit or roll back together.
		events.publishEvent(new ProjectDeletedEvent(id));
		projects.delete(project);
	}

	private Project load(UUID id) {
		return projects.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
	}

}
