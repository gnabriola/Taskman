package com.taskman.label;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Every query is scoped by owner, so another user's labels are never returned. */
interface LabelRepository extends JpaRepository<Label, UUID> {

	List<Label> findAllByOwnerIdOrderByNameAsc(UUID ownerId);

	Optional<Label> findByIdAndOwnerId(UUID id, UUID ownerId);

	Optional<Label> findByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);

	@Query("select l.id from Label l where l.ownerId = :ownerId and l.id in :ids")
	List<UUID> findExistingIds(UUID ownerId, Collection<UUID> ids);

}
