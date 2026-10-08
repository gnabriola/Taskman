package com.taskman.task;

import com.taskman.project.ProjectDeletedEvent;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Deletes a project's tasks when the project is deleted. Runs synchronously inside
 * the project's delete transaction, before the project row is removed, so the
 * task.project_id foreign key is never violated.
 */
@Component
class ProjectDeletionListener {

	private final TaskRepository tasks;

	ProjectDeletionListener(TaskRepository tasks) {
		this.tasks = tasks;
	}

	@EventListener
	void on(ProjectDeletedEvent event) {
		tasks.deleteByProjectId(event.projectId());
	}

}
