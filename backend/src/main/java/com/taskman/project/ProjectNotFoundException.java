package com.taskman.project;

import java.util.UUID;

import com.taskman.shared.NotFoundException;

public class ProjectNotFoundException extends NotFoundException {

	public ProjectNotFoundException(UUID projectId) {
		super("Project " + projectId + " does not exist");
	}

}
