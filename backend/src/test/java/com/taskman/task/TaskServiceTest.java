package com.taskman.task;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.taskman.label.LabelApi;
import com.taskman.project.ProjectApi;
import com.taskman.project.ProjectNotFoundException;
import com.taskman.task.recurrence.Recurrence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

	private static final UUID OWNER_ID = UUID.randomUUID();

	@Mock
	private TaskRepository tasks;

	@Mock
	private ProjectApi projects;

	@Mock
	private LabelApi labels;

	private TaskService taskService;

	@BeforeEach
	void createService() {
		taskService = serviceAt("2026-10-07T10:00:00Z");
	}

	@Test
	void createRejectsMissingProject() {
		given(projects.exists(OWNER_ID, PROJECT_ID)).willReturn(false);

		assertThatThrownBy(() -> taskService.create(OWNER_ID, PROJECT_ID, new CreateTaskRequest("Pay rent", null)))
			.isInstanceOf(ProjectNotFoundException.class);
		verify(tasks, never()).save(any());
	}

	@Test
	void createStripsTitleAndDropsBlankDescription() {
		given(projects.exists(OWNER_ID, PROJECT_ID)).willReturn(true);
		given(tasks.save(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));

		TaskResponse created = taskService.create(OWNER_ID, PROJECT_ID, new CreateTaskRequest("  Pay rent ", "   "));

		assertThat(created.title()).isEqualTo("Pay rent");
		assertThat(created.description()).isNull();
		assertThat(created.completed()).isFalse();
		assertThat(created.priority()).isEqualTo(4);
	}

	@Test
	void updateWithinSameProjectDoesNotAskProjectModule() {
		given(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).willReturn(Optional.of(task("Old")));

		taskService.update(OWNER_ID, TASK_ID, new UpdateTaskRequest("New", null, PROJECT_ID));

		verifyNoInteractions(projects);
	}

	@Test
	void updateRejectsMoveToMissingProject() {
		UUID otherProject = UUID.randomUUID();
		given(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).willReturn(Optional.of(task("Task")));
		given(projects.exists(OWNER_ID, otherProject)).willReturn(false);

		assertThatThrownBy(() -> taskService.update(OWNER_ID, TASK_ID, new UpdateTaskRequest("Task", null, otherProject)))
			.isInstanceOf(ProjectNotFoundException.class);
	}

	@Test
	void completingTwiceKeepsFirstCompletionTime() {
		given(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).willReturn(Optional.of(task("Task")));

		TaskResponse first = taskService.complete(OWNER_ID, TASK_ID, null);
		TaskResponse second = taskService.complete(OWNER_ID, TASK_ID, null);

		assertThat(first.completedAt()).isNotNull();
		assertThat(second.completedAt()).isEqualTo(first.completedAt());
	}

	@Test
	void reopenClearsCompletion() {
		Task task = task("Task");
		task.complete(LocalDate.of(2026, 10, 7));
		given(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).willReturn(Optional.of(task));

		TaskResponse reopened = taskService.reopen(OWNER_ID, TASK_ID);

		assertThat(reopened.completed()).isFalse();
		assertThat(reopened.completedAt()).isNull();
	}

	@Test
	void quickAddInterpretsTodayInTheUsersTimeZone() {
		// 23:30 UTC on Oct 7 is already Oct 8 in Berlin
		TaskService service = serviceAt("2026-10-07T23:30:00Z");
		given(projects.exists(OWNER_ID, PROJECT_ID)).willReturn(true);
		given(tasks.save(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));

		TaskResponse created = service.quickAdd(OWNER_ID, new QuickAddRequest("call mom today p2", PROJECT_ID, "Europe/Berlin"));

		assertThat(created.title()).isEqualTo("call mom");
		assertThat(created.dueDate()).isEqualTo(LocalDate.of(2026, 10, 8));
		assertThat(created.priority()).isEqualTo(2);
		assertThat(created.projectId()).isEqualTo(PROJECT_ID);
	}

	@Test
	void quickAddResolvesProjectByName() {
		UUID work = UUID.randomUUID();
		TaskService service = serviceAt("2026-10-07T10:00:00Z");
		given(projects.findIdByName(OWNER_ID, "Work")).willReturn(Optional.of(work));
		given(tasks.save(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));

		TaskResponse created = service.quickAdd(OWNER_ID, new QuickAddRequest("write report #Work", PROJECT_ID, "UTC"));

		assertThat(created.projectId()).isEqualTo(work);
		verify(projects, never()).exists(any(), any());
	}

	@Test
	void quickAddRejectsUnknownProject() {
		TaskService service = serviceAt("2026-10-07T10:00:00Z");
		given(projects.findIdByName(OWNER_ID, "hoem")).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.quickAdd(OWNER_ID, new QuickAddRequest("pay rent #hoem", PROJECT_ID, "UTC")))
			.isInstanceOf(QuickAddRejectedException.class)
			.hasMessage("No project named 'hoem'");
		verify(tasks, never()).save(any());
	}

	@Test
	void quickAddRejectsTextWithoutTitle() {
		TaskService service = serviceAt("2026-10-07T10:00:00Z");

		assertThatThrownBy(() -> service.quickAdd(OWNER_ID, new QuickAddRequest("tomorrow 5pm p1", PROJECT_ID, "UTC")))
			.isInstanceOf(QuickAddRejectedException.class);
		verifyNoInteractions(tasks);
	}

	@Test
	void completingRecurringTaskMovesItToTheNextOccurrence() {
		Task task = new Task(OWNER_ID, new TaskContent(PROJECT_ID, "Water plants", null, LocalDate.of(2026, 10, 7), null,
				TaskContent.DEFAULT_PRIORITY, Recurrence.parse("FREQ=DAILY", false), null));
		given(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).willReturn(Optional.of(task));

		TaskResponse completed = taskService.complete(OWNER_ID, TASK_ID, "UTC");

		assertThat(completed.completed()).isFalse();
		assertThat(completed.dueDate()).isEqualTo(LocalDate.of(2026, 10, 8));
	}

	@Test
	void completingRecurringTaskUsesTodayInTheUsersTimeZone() {
		// 23:30 UTC on Oct 7 is already Oct 8 in Berlin, so "every! day" counts from Oct 8
		TaskService service = serviceAt("2026-10-07T23:30:00Z");
		Task task = new Task(OWNER_ID, new TaskContent(PROJECT_ID, "Stretch", null, LocalDate.of(2026, 10, 1), null,
				TaskContent.DEFAULT_PRIORITY, Recurrence.parse("FREQ=DAILY", true), null));
		given(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).willReturn(Optional.of(task));

		assertThat(service.complete(OWNER_ID, TASK_ID, "Europe/Berlin").dueDate()).isEqualTo(LocalDate.of(2026, 10, 9));
	}

	@Test
	void missingTaskThrowsNotFound() {
		given(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.findById(OWNER_ID, TASK_ID)).isInstanceOf(TaskNotFoundException.class);
	}

	private TaskService serviceAt(String instant) {
		return new TaskService(tasks, projects, labels, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
	}

	private static Task task(String title) {
		return new Task(OWNER_ID, new TaskContent(PROJECT_ID, title, null, null, null, TaskContent.DEFAULT_PRIORITY, null, null));
	}

}
