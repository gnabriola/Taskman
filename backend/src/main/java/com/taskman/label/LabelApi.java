package com.taskman.label;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * What the label module offers to other modules. Other modules depend on this
 * interface only, never on {@code Label} or {@code LabelRepository}.
 */
public interface LabelApi {

	/** @throws LabelNotFoundException if any id does not exist or is another user's */
	void requireExisting(UUID ownerId, Set<UUID> labelIds);

	/** Finds one of the user's labels by name, ignoring case. */
	Optional<UUID> findIdByName(UUID ownerId, String name);

	/**
	 * Ids of the user's labels with these names (ignoring case), in the same order.
	 * Labels that do not exist yet are created: labels are lightweight tags.
	 */
	List<UUID> findOrCreateByNames(UUID ownerId, Collection<String> names);

}
