package com.taskman.task;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface TaskRepository extends JpaRepository<Task, UUID> {

	List<Task> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

	List<Task> findByProjectIdAndCompletedAtIsNullOrderByCreatedAtAsc(UUID projectId);

	List<Task> findByProjectIdAndCompletedAtIsNotNullOrderByCreatedAtAsc(UUID projectId);

	// task_label is a collection table, not an entity, so this is plain SQL.
	@Modifying
	@Query(value = "delete from task_label where label_id = :labelId", nativeQuery = true)
	void removeLabelFromAllTasks(UUID labelId);

	// One DELETE statement instead of loading every task and deleting them one by one.
	@Modifying
	@Query("delete from Task t where t.projectId = :projectId")
	void deleteByProjectId(UUID projectId);

}
