package com.taskman.label;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What the label module offers to other modules. Other modules depend on this
 * interface only, never on {@code Label} or {@code LabelRepository}.
 */
public interface LabelApi {

	/** @throws LabelNotFoundException if any of the ids does not exist */
	void requireExisting(Set<UUID> labelIds);

	/**
	 * Ids of the labels with these names (ignoring case), in the same order. Labels
	 * that do not exist yet are created: labels are lightweight tags.
	 */
	List<UUID> findOrCreateByNames(Collection<String> names);

}
