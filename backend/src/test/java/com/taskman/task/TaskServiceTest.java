package com.taskman.task;

import java.util.Optional;
import java.util.UUID;

import com.taskman.project.ProjectApi;
import com.taskman.project.ProjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	private static final UUID PROJECT_ID = UUID.randomUUID();

	private static final UUID TASK_ID = UUID.randomUUID();

	@Mock
	private TaskRepository tasks;

	@Mock
	private ProjectApi projects;

	@InjectMocks
	private TaskService taskService;

	@Test
	void createRejectsMissingProject() {
		given(projects.exists(PROJECT_ID)).willReturn(false);

		assertThatThrownBy(() -> taskService.create(PROJECT_ID, new CreateTaskRequest("Pay rent", null)))
			.isInstanceOf(ProjectNotFoundException.class);
		verify(tasks, never()).save(any());
	}

	@Test
	void createStripsTitleAndDropsBlankDescription() {
		given(projects.exists(PROJECT_ID)).willReturn(true);
		given(tasks.save(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));

		TaskResponse created = taskService.create(PROJECT_ID, new CreateTaskRequest("  Pay rent ", "   "));

		assertThat(created.title()).isEqualTo("Pay rent");
		assertThat(created.description()).isNull();
		assertThat(created.completed()).isFalse();
		assertThat(created.priority()).isEqualTo(4);
	}

	@Test
	void updateWithinSameProjectDoesNotAskProjectModule() {
		given(tasks.findById(TASK_ID)).willReturn(Optional.of(task("Old")));

		taskService.update(TASK_ID, new UpdateTaskRequest("New", null, PROJECT_ID));

		verifyNoInteractions(projects);
	}

	@Test
	void updateRejectsMoveToMissingProject() {
		UUID otherProject = UUID.randomUUID();
		given(tasks.findById(TASK_ID)).willReturn(Optional.of(task("Task")));
		given(projects.exists(otherProject)).willReturn(false);

		assertThatThrownBy(() -> taskService.update(TASK_ID, new UpdateTaskRequest("Task", null, otherProject)))
			.isInstanceOf(ProjectNotFoundException.class);
	}

	@Test
	void completingTwiceKeepsFirstCompletionTime() {
		given(tasks.findById(TASK_ID)).willReturn(Optional.of(task("Task")));

		TaskResponse first = taskService.complete(TASK_ID);
		TaskResponse second = taskService.complete(TASK_ID);

		assertThat(first.completedAt()).isNotNull();
		assertThat(second.completedAt()).isEqualTo(first.completedAt());
	}

	@Test
	void reopenClearsCompletion() {
		Task task = task("Task");
		task.complete();
		given(tasks.findById(TASK_ID)).willReturn(Optional.of(task));

		TaskResponse reopened = taskService.reopen(TASK_ID);

		assertThat(reopened.completed()).isFalse();
		assertThat(reopened.completedAt()).isNull();
	}

	@Test
	void missingTaskThrowsNotFound() {
		given(tasks.findById(TASK_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.findById(TASK_ID)).isInstanceOf(TaskNotFoundException.class);
	}

	private static Task task(String title) {
		return new Task(new TaskContent(PROJECT_ID, title, null, null, null, TaskContent.DEFAULT_PRIORITY));
	}

}
