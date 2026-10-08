package com.taskman.project;

import java.util.Optional;
import java.util.UUID;

/**
 * What the project module offers to other modules. Other modules depend on this
 * interface only, never on {@code Project} or {@code ProjectRepository}.
 */
public interface ProjectApi {

	boolean exists(UUID projectId);

	/**
	 * Finds a project by name, ignoring case. Names are not unique; if several
	 * projects share a name, the oldest one wins.
	 */
	Optional<UUID> findIdByName(String name);

}
