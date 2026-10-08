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

	// One DELETE statement instead of loading every task and deleting them one by one.
	@Modifying
	@Query("delete from Task t where t.projectId = :projectId")
	void deleteByProjectId(UUID projectId);

}
