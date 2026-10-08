package com.taskman.project;

import com.jayway.jsonpath.JsonPath;
import com.taskman.DatabaseCleaner;
import com.taskman.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ProjectApiTests {

	private static final String MISSING_ID = "0199c3a0-0000-7000-8000-000000000000";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	@BeforeEach
	void cleanDatabase() {
		databaseCleaner.clean();
	}

	@Test
	void createsProjectAndReturnsLocation() throws Exception {
		var result = mvc.post()
			.uri("/api/projects")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "  Home  "}
					""")
			.exchange();

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Home");
		String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		assertThat(result.getResponse().getHeader("Location")).endsWith("/api/projects/" + id);

		assertThat(mvc.get().uri("/api/projects/{id}", id)).hasStatusOk()
			.bodyJson()
			.extractingPath("$.name")
			.isEqualTo("Home");
	}

	@Test
	void rejectsBlankName() {
		assertThat(mvc.post().uri("/api/projects").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "   "}
				""")).hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("name");
	}

	@Test
	void rejectsTooLongName() {
		String name = "x".repeat(121);
		assertThat(mvc.post()
			.uri("/api/projects")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"" + name + "\"}")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void listsProjectsInCreationOrder() throws Exception {
		createProject("Work");
		createProject("Home");

		assertThat(mvc.get().uri("/api/projects")).hasStatusOk()
			.bodyJson()
			.extractingPath("$[*].name")
			.asArray()
			.containsExactly("Work", "Home");
	}

	@Test
	void renamesProject() throws Exception {
		String id = createProject("Wrok");

		assertThat(mvc.put().uri("/api/projects/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Work"}
				""")).hasStatusOk().bodyJson().extractingPath("$.name").isEqualTo("Work");
	}

	@Test
	void returnsProblemDetailForMissingProject() {
		assertThat(mvc.get().uri("/api/projects/{id}", MISSING_ID)).hasStatus(HttpStatus.NOT_FOUND)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Project " + MISSING_ID + " does not exist");
	}

	@Test
	void rejectsMalformedId() {
		assertThat(mvc.get().uri("/api/projects/not-a-uuid")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void deletesProject() throws Exception {
		String id = createProject("Old");

		assertThat(mvc.delete().uri("/api/projects/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.get().uri("/api/projects/{id}", id)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void deletingMissingProjectReturnsNotFound() {
		assertThat(mvc.delete().uri("/api/projects/{id}", MISSING_ID)).hasStatus(HttpStatus.NOT_FOUND);
	}

	private String createProject(String name) throws Exception {
		var result = mvc.post()
			.uri("/api/projects")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"" + name + "\"}")
			.exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

}
