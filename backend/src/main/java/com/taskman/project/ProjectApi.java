package com.taskman.project;

import java.util.UUID;

/**
 * What the project module offers to other modules. Other modules depend on this
 * interface only, never on {@code Project} or {@code ProjectRepository}.
 */
public interface ProjectApi {

	boolean exists(UUID projectId);

}
