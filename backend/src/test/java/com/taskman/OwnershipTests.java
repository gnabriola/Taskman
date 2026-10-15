package com.taskman;

import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.taskman.user.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Two users: Ada (the default test user) owns a project, a label and a task. Eve must
 * not be able to see or touch any of it, through any endpoint. Another user's data is
 * always 404 (or 422 when referenced by name), never 403, so ids cannot be probed.
 */
@IntegrationTest
class OwnershipTests {

	private static final UUID EVE_ID = UUID.fromString("0199c3a0-0000-7000-8000-00000000eeee");

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	@Autowired
	private JdbcTemplate jdbc;

	private String adaProject;

	private String adaLabel;

	private String adaTask;

	private String eveProject;

	@BeforeEach
	void setUp() throws Exception {
		databaseCleaner.clean();
		jdbc.update("""
				INSERT INTO app_user (id, email, display_name, password_hash, created_at, updated_at)
				VALUES (?, 'eve@example.com', 'Eve', '{noop}unused', now(), now())
				""", EVE_ID);
		adaProject = idOf(ada(mvc.post().uri("/api/projects")).content("{\"name\": \"Home\"}"));
		adaLabel = idOf(ada(mvc.post().uri("/api/labels")).content("{\"name\": \"errands\"}"));
		adaTask = idOf(ada(mvc.post().uri("/api/projects/{id}/tasks", adaProject)).content("""
				{"title": "Ada's secret", "dueDate": "2030-01-01", "labelIds": ["%s"]}""".formatted(adaLabel)));
		eveProject = idOf(eve(mvc.post().uri("/api/projects")).content("{\"name\": \"Eve's\"}"));
	}

	@Test
	void listsOnlyTheUsersOwnData() {
		assertThat(eve(mvc.get().uri("/api/projects"))).bodyJson()
			.extractingPath("$[*].name")
			.asArray()
			.containsExactly("Eve's");
		assertThat(eve(mvc.get().uri("/api/labels"))).bodyJson().extractingPath("$").asArray().isEmpty();
		assertThat(eve(mvc.get().uri("/api/tasks").param("filter", "no date | !no date"))).hasStatusOk()
			.bodyJson()
			.extractingPath("$")
			.asArray()
			.isEmpty();
		assertThat(ada(mvc.get().uri("/api/tasks").param("filter", "no date | !no date"))).bodyJson()
			.extractingPath("$[*].title")
			.asArray()
			.containsExactly("Ada's secret");
	}

	@Test
	void anotherUsersProjectIsNotFound() {
		assertThat(eve(mvc.get().uri("/api/projects/{id}", adaProject))).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(eve(mvc.put().uri("/api/projects/{id}", adaProject)).content("{\"name\": \"Mine now\"}"))
			.hasStatus(HttpStatus.NOT_FOUND);
		assertThat(eve(mvc.delete().uri("/api/projects/{id}", adaProject))).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(eve(mvc.get().uri("/api/projects/{id}/tasks", adaProject))).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(eve(mvc.post().uri("/api/projects/{id}/tasks", adaProject)).content("{\"title\": \"Spam\"}"))
			.hasStatus(HttpStatus.NOT_FOUND);
		assertThat(ada(mvc.get().uri("/api/projects/{id}", adaProject))).hasStatusOk()
			.bodyJson()
			.extractingPath("$.name")
			.isEqualTo("Home");
	}

	@Test
	void anotherUsersTaskIsNotFound() {
		assertThat(eve(mvc.get().uri("/api/tasks/{id}", adaTask))).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(eve(mvc.put().uri("/api/tasks/{id}", adaTask)).content("""
				{"title": "Mine now", "projectId": "%s"}""".formatted(eveProject))).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(eve(mvc.post().uri("/api/tasks/{id}/complete", adaTask))).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(eve(mvc.post().uri("/api/tasks/{id}/reopen", adaTask))).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(eve(mvc.delete().uri("/api/tasks/{id}", adaTask))).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(ada(mvc.get().uri("/api/tasks/{id}", adaTask))).hasStatusOk()
			.bodyJson()
			.extractingPath("$.completed")
			.isEqualTo(false);
	}

	@Test
	void anotherUsersLabelIsNotFound() {
		assertThat(eve(mvc.put().uri("/api/labels/{id}", adaLabel)).content("{\"name\": \"stolen\"}"))
			.hasStatus(HttpStatus.NOT_FOUND);
		assertThat(eve(mvc.delete().uri("/api/labels/{id}", adaLabel))).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(eve(mvc.post().uri("/api/projects/{id}/tasks", eveProject)).content("""
				{"title": "Borrowing a label", "labelIds": ["%s"]}""".formatted(adaLabel)))
			.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void cannotMoveOwnTaskIntoAnotherUsersProject() throws Exception {
		String eveTask = idOf(eve(mvc.post().uri("/api/projects/{id}/tasks", eveProject)).content("{\"title\": \"Mine\"}"));

		assertThat(eve(mvc.put().uri("/api/tasks/{id}", eveTask)).content("""
				{"title": "Mine", "projectId": "%s"}""".formatted(adaProject))).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void namesInQuickAddAndFiltersOnlySeeTheUsersOwnData() throws Exception {
		assertThat(eve(mvc.post().uri("/api/tasks/quick-add")).content(quickAdd("Sneak in #Home")))
			.hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(eve(mvc.get().uri("/api/tasks").param("filter", "#Home")))
			.hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(eve(mvc.get().uri("/api/tasks").param("filter", "@errands")))
			.hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);

		// @errands in Eve's quick-add creates Eve's own label, not a link to Ada's.
		MvcTestResult added = eve(mvc.post().uri("/api/tasks/quick-add")).content(quickAdd("Buy milk @errands"))
			.exchange();
		assertThat(added).hasStatus(HttpStatus.CREATED);
		List<String> labelIds = JsonPath.read(added.getResponse().getContentAsString(), "$.labelIds");
		assertThat(labelIds).hasSize(1).doesNotContain(adaLabel);
	}

	@Test
	void usersCanReuseEachOthersNames() {
		assertThat(eve(mvc.post().uri("/api/projects")).content("{\"name\": \"Home\"}"))
			.hasStatus(HttpStatus.CREATED);
		assertThat(eve(mvc.post().uri("/api/labels")).content("{\"name\": \"Errands\"}"))
			.hasStatus(HttpStatus.CREATED);
	}

	private String quickAdd(String text) {
		return """
				{"text": "%s", "defaultProjectId": "%s", "timeZone": "UTC"}""".formatted(text, eveProject);
	}

	private MockMvcRequestBuilder ada(MockMvcRequestBuilder request) {
		return request.with(user(TestUsers.principal())).contentType(MediaType.APPLICATION_JSON);
	}

	private MockMvcRequestBuilder eve(MockMvcRequestBuilder request) {
		return request.with(user(TestUsers.principal(EVE_ID, "eve@example.com", "Eve")))
			.contentType(MediaType.APPLICATION_JSON);
	}

	private static String idOf(MockMvcRequestBuilder request) throws Exception {
		MvcTestResult result = request.exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

}
