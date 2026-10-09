package com.taskman.task;

import java.time.LocalDate;
import java.time.ZoneId;

import com.jayway.jsonpath.JsonPath;
import com.taskman.DatabaseCleaner;
import com.taskman.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class TaskApiTests {

	private static final String MISSING_ID = "0199c3a0-0000-7000-8000-000000000000";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	private String projectId;

	@BeforeEach
	void setUp() throws Exception {
		databaseCleaner.clean();
		projectId = createProject("Home");
	}

	@Test
	void createsTaskInProject() throws Exception {
		MvcTestResult result = mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Pay rent", "description": "Before the 5th"}
					""")
			.exchange();

		assertThat(result).hasStatus(HttpStatus.CREATED);
		String id = idOf(result);
		assertThat(result.getResponse().getHeader("Location")).endsWith("/api/tasks/" + id);
		assertThat(mvc.get().uri("/api/tasks/{id}", id)).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"projectId": "%s", "title": "Pay rent", "description": "Before the 5th",
					 "completed": false, "completedAt": null}
					""".formatted(projectId));
	}

	@Test
	void storesDueDateTimeAndPriority() {
		assertThat(mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Pay rent", "dueDate": "2026-10-08", "dueTime": "17:00", "priority": 1}
					""")).hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.isLenientlyEqualTo("""
					{"dueDate": "2026-10-08", "dueTime": "17:00:00", "priority": 1}
					""");
	}

	@Test
	void rejectsDueTimeWithoutDueDate() {
		assertThat(mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Pay rent", "dueTime": "17:00"}
					""")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("dueTime");
	}

	@Test
	void rejectsPriorityOutOfRange() {
		assertThat(mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Pay rent", "priority": 5}
					""")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void updateReplacesDueDateAndPriority() throws Exception {
		MvcTestResult created = mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Pay rent", "dueDate": "2026-10-08", "priority": 1}
					""")
			.exchange();
		String id = idOf(created);

		assertThat(mvc.put().uri("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"title": "Pay rent", "projectId": "%s"}
				""".formatted(projectId))).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"dueDate": null, "dueTime": null, "priority": 4}
					""");
	}

	@Test
	void storesRecurrenceAndReturnsALabel() {
		assertThat(mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Gym", "dueDate": "2030-01-07", "recurrence": "FREQ=WEEKLY;BYDAY=FR,MO"}
					""")).hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.isLenientlyEqualTo("""
					{"recurrence": "FREQ=WEEKLY;BYDAY=MO,FR", "recurrenceFromCompletion": false,
					 "recurrenceLabel": "every Mon, Fri"}
					""");
	}

	@Test
	void rejectsUnsupportedRecurrence() {
		assertThat(mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Gym", "dueDate": "2030-01-07", "recurrence": "FREQ=HOURLY"}
					""")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("recurrence");
	}

	@Test
	void rejectsRecurrenceWithoutDueDate() {
		assertThat(mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Gym", "recurrence": "FREQ=DAILY"}
					""")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("dueDate");
	}

	@Test
	void completingRecurringTaskKeepsItOpenWithTheNextDueDate() throws Exception {
		String id = createRecurring("2030-01-07", "FREQ=WEEKLY;BYDAY=MO,FR");

		assertThat(mvc.post().uri("/api/tasks/{id}/complete", id)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"completed": false, "completedAt": null, "dueDate": "2030-01-11"}
				""");
		assertThat(mvc.get().uri("/api/projects/{projectId}/tasks", projectId).param("completed", "false"))
			.bodyJson()
			.extractingPath("$[0].dueDate")
			.isEqualTo("2030-01-11");
	}

	@Test
	void completingOverdueRecurringTaskSkipsToAfterToday() throws Exception {
		String id = createRecurring("2020-01-01", "FREQ=DAILY");
		String tomorrow = LocalDate.now(ZoneId.of("Pacific/Kiritimati")).plusDays(1).toString();

		assertThat(mvc.post().uri("/api/tasks/{id}/complete", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"timeZone": "Pacific/Kiritimati"}
				""")).hasStatusOk().bodyJson().extractingPath("$.dueDate").isEqualTo(tomorrow);
	}

	@Test
	void completeRejectsInvalidTimeZone() throws Exception {
		String id = createRecurring("2030-01-07", "FREQ=DAILY");

		assertThat(mvc.post().uri("/api/tasks/{id}/complete", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"timeZone": "Mars/Olympus"}
				""")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void removingRecurrenceMakesTaskCompletable() throws Exception {
		String id = createRecurring("2030-01-07", "FREQ=DAILY");

		assertThat(mvc.put().uri("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"title": "Task", "projectId": "%s", "dueDate": "2030-01-07"}
				""".formatted(projectId))).hasStatusOk().bodyJson().extractingPath("$.recurrence").isNull();
		assertThat(mvc.post().uri("/api/tasks/{id}/complete", id)).bodyJson()
			.extractingPath("$.completed")
			.isEqualTo(true);
	}

	@Test
	void storesLabels() throws Exception {
		String errands = createLabel("errands");
		String urgent = createLabel("urgent");

		MvcTestResult result = mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Buy milk", "labelIds": ["%s", "%s"]}
					""".formatted(urgent, errands))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.extractingPath("$.labelIds")
			.asArray()
			.containsExactlyInAnyOrder(errands, urgent);
	}

	@Test
	void rejectsUnknownLabel() {
		assertThat(mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Buy milk", "labelIds": ["%s"]}
					""".formatted(MISSING_ID))).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void updateReplacesLabels() throws Exception {
		String errands = createLabel("errands");
		String home = createLabel("home");
		String id = createTaskWithLabels("Buy milk", errands);

		assertThat(mvc.put().uri("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"title": "Buy milk", "projectId": "%s", "labelIds": ["%s"]}
				""".formatted(projectId, home))).hasStatusOk()
			.bodyJson()
			.extractingPath("$.labelIds")
			.asArray()
			.containsExactly(home);
	}

	@Test
	void deletingLabelRemovesItFromTasks() throws Exception {
		String errands = createLabel("errands");
		String id = createTaskWithLabels("Buy milk", errands);

		assertThat(mvc.delete().uri("/api/labels/{id}", errands)).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(mvc.get().uri("/api/tasks/{id}", id)).hasStatusOk()
			.bodyJson()
			.extractingPath("$.labelIds")
			.asArray()
			.isEmpty();
	}

	@Test
	void deletingProjectAlsoDeletesLabelLinksOfItsTasks() throws Exception {
		String errands = createLabel("errands");
		createTaskWithLabels("Buy milk", errands);

		assertThat(mvc.delete().uri("/api/projects/{id}", projectId)).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.get().uri("/api/labels")).bodyJson().extractingPath("$[*].name").asArray().containsExactly(
				"errands");
	}

	@Test
	void creatingTaskInMissingProjectReturnsNotFound() {
		assertThat(mvc.post()
			.uri("/api/projects/{projectId}/tasks", MISSING_ID)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Orphan"}
					""")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void rejectsBlankTitle() {
		assertThat(mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": ""}
					""")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("title");
	}

	@Test
	void filtersTasksByCompletion() throws Exception {
		String done = createTask(projectId, "Done");
		createTask(projectId, "Open");
		assertThat(mvc.post().uri("/api/tasks/{id}/complete", done)).hasStatusOk();

		assertThat(mvc.get().uri("/api/projects/{projectId}/tasks", projectId).param("completed", "false"))
			.bodyJson()
			.extractingPath("$[*].title")
			.asArray()
			.containsExactly("Open");
		assertThat(mvc.get().uri("/api/projects/{projectId}/tasks", projectId).param("completed", "true"))
			.bodyJson()
			.extractingPath("$[*].title")
			.asArray()
			.containsExactly("Done");
		assertThat(mvc.get().uri("/api/projects/{projectId}/tasks", projectId)).bodyJson()
			.extractingPath("$[*].title")
			.asArray()
			.containsExactly("Done", "Open");
	}

	@Test
	void listingTasksOfMissingProjectReturnsNotFound() {
		assertThat(mvc.get().uri("/api/projects/{projectId}/tasks", MISSING_ID)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void completeIsIdempotentAndReopenUndoesIt() throws Exception {
		String id = createTask(projectId, "Pay rent");

		MvcTestResult first = mvc.post().uri("/api/tasks/{id}/complete", id).exchange();
		MvcTestResult second = mvc.post().uri("/api/tasks/{id}/complete", id).exchange();

		assertThat(first).hasStatusOk().bodyJson().extractingPath("$.completed").isEqualTo(true);
		String completedAt = JsonPath.read(first.getResponse().getContentAsString(), "$.completedAt");
		assertThat(second).bodyJson().extractingPath("$.completedAt").isEqualTo(completedAt);

		assertThat(mvc.post().uri("/api/tasks/{id}/reopen", id)).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"completed": false, "completedAt": null}
					""");
	}

	@Test
	void updateMovesTaskToAnotherProject() throws Exception {
		String id = createTask(projectId, "Buy paint");
		String work = createProject("Work");

		assertThat(mvc.put().uri("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"title": "Buy paint for office", "projectId": "%s"}
				""".formatted(work))).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"title": "Buy paint for office", "projectId": "%s"}
					""".formatted(work));
		assertThat(mvc.get().uri("/api/projects/{projectId}/tasks", projectId)).bodyJson()
			.extractingPath("$")
			.asArray()
			.isEmpty();
	}

	@Test
	void updateRejectsMoveToMissingProject() throws Exception {
		String id = createTask(projectId, "Task");

		assertThat(mvc.put().uri("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"title": "Task", "projectId": "%s"}
				""".formatted(MISSING_ID))).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void deletesTask() throws Exception {
		String id = createTask(projectId, "Task");

		assertThat(mvc.delete().uri("/api/tasks/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.get().uri("/api/tasks/{id}", id)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void deletingProjectDeletesOnlyItsTasks() throws Exception {
		String doomed = createTask(projectId, "Doomed");
		String work = createProject("Work");
		String survivor = createTask(work, "Survivor");

		assertThat(mvc.delete().uri("/api/projects/{id}", projectId)).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(mvc.get().uri("/api/tasks/{id}", doomed)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.get().uri("/api/tasks/{id}", survivor)).hasStatusOk();
	}

	private String createProject(String name) throws Exception {
		MvcTestResult result = mvc.post()
			.uri("/api/projects")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"" + name + "\"}")
			.exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return idOf(result);
	}

	private String createTask(String projectId, String title) throws Exception {
		MvcTestResult result = mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\": \"" + title + "\"}")
			.exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return idOf(result);
	}

	private String createRecurring(String dueDate, String rule) throws Exception {
		MvcTestResult result = mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Task", "dueDate": "%s", "recurrence": "%s"}
					""".formatted(dueDate, rule))
			.exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return idOf(result);
	}

	private String createLabel(String name) throws Exception {
		MvcTestResult result = mvc.post()
			.uri("/api/labels")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"" + name + "\"}")
			.exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return idOf(result);
	}

	private String createTaskWithLabels(String title, String... labelIds) throws Exception {
		String ids = String.join("\", \"", labelIds);
		MvcTestResult result = mvc.post()
			.uri("/api/projects/{projectId}/tasks", projectId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\": \"" + title + "\", \"labelIds\": [\"" + ids + "\"]}")
			.exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return idOf(result);
	}

	private static String idOf(MvcTestResult result) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

}
