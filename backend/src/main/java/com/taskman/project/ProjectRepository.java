package com.taskman.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Every query is scoped by owner, so another user's projects are never returned. */
interface ProjectRepository extends JpaRepository<Project, UUID> {

	List<Project> findAllByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

	Optional<Project> findByIdAndOwnerId(UUID id, UUID ownerId);

	boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

	Optional<Project> findFirstByOwnerIdAndNameIgnoreCaseOrderByCreatedAtAsc(UUID ownerId, String name);

}
