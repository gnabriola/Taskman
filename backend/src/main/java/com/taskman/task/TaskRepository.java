package com.taskman.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Lookups are scoped by owner. The project-wide queries are only called after the
 * project's ownership was checked, but they include the owner anyway (defence in depth).
 */
interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

	Optional<Task> findByIdAndOwnerId(UUID id, UUID ownerId);

	List<Task> findByOwnerIdAndProjectIdOrderByCreatedAtAsc(UUID ownerId, UUID projectId);

	List<Task> findByOwnerIdAndProjectIdAndCompletedAtIsNullOrderByCreatedAtAsc(UUID ownerId, UUID projectId);

	List<Task> findByOwnerIdAndProjectIdAndCompletedAtIsNotNullOrderByCreatedAtAsc(UUID ownerId, UUID projectId);

	// task_label is a collection table, not an entity, so this is plain SQL.
	@Modifying
	@Query(value = "delete from task_label where label_id = :labelId", nativeQuery = true)
	void removeLabelFromAllTasks(UUID labelId);

	// One DELETE statement instead of loading every task and deleting them one by one.
	@Modifying
	@Query("delete from Task t where t.projectId = :projectId")
	void deleteByProjectId(UUID projectId);

}
