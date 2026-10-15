package com.taskman.project;

import java.util.Optional;
import java.util.UUID;

/**
 * What the project module offers to other modules. Other modules depend on this
 * interface only, never on {@code Project} or {@code ProjectRepository}.
 */
public interface ProjectApi {

	/** Whether the project exists and belongs to this user. */
	boolean exists(UUID ownerId, UUID projectId);

	/**
	 * Finds one of the user's projects by name, ignoring case. Names are not unique; if
	 * several projects share a name, the oldest one wins.
	 */
	Optional<UUID> findIdByName(UUID ownerId, String name);

}
