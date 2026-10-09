package com.taskman.label;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface LabelRepository extends JpaRepository<Label, UUID> {

	List<Label> findAllByOrderByNameAsc();

	Optional<Label> findByNameIgnoreCase(String name);

	@Query("select l.id from Label l where l.id in :ids")
	List<UUID> findExistingIds(Collection<UUID> ids);

}
