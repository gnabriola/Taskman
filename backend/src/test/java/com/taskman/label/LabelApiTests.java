package com.taskman.label;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class LabelApiTests {

	private static final String MISSING_ID = "0199c3a0-0000-7000-8000-000000000000";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	@Autowired
	private LabelApi labelApi;

	@BeforeEach
	void cleanDatabase() {
		databaseCleaner.clean();
	}

	@Test
	void createsLabelAndReturnsLocation() throws Exception {
		MvcTestResult result = post("errands");

		assertThat(result).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.name").isEqualTo("errands");
		String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		assertThat(result.getResponse().getHeader("Location")).endsWith("/api/labels/" + id);
	}

	@Test
	void rejectsDuplicateNameIgnoringCase() {
		post("errands");

		assertThat(post("Errands")).hasStatus(HttpStatus.CONFLICT)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("A label named 'Errands' already exists");
	}

	@Test
	void rejectsNamesThatCannotBeWrittenAsAtName() {
		for (String name : List.of("two words", "@home", "a#b", " ")) {
			assertThat(post(name)).as(name).hasStatus(HttpStatus.BAD_REQUEST);
		}
	}

	@Test
	void listsLabelsByName() {
		post("work");
		post("errands");

		assertThat(mvc.get().uri("/api/labels")).hasStatusOk()
			.bodyJson()
			.extractingPath("$[*].name")
			.asArray()
			.containsExactly("errands", "work");
	}

	@Test
	void renamesLabelIncludingChangingCase() throws Exception {
		String id = idOf(post("errands"));

		assertThat(put(id, "Errands")).hasStatusOk().bodyJson().extractingPath("$.name").isEqualTo("Errands");
	}

	@Test
	void rejectsRenameToAnotherLabelsName() throws Exception {
		post("work");
		String id = idOf(post("errands"));

		assertThat(put(id, "WORK")).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void deletesLabel() throws Exception {
		String id = idOf(post("old"));

		assertThat(mvc.delete().uri("/api/labels/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.delete().uri("/api/labels/{id}", id)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void findOrCreateReusesExistingLabelsIgnoringCaseAndCreatesMissingOnes() throws Exception {
		UUID errands = UUID.fromString(idOf(post("errands")));

		List<UUID> ids = labelApi.findOrCreateByNames(List.of("Errands", "urgent"));

		assertThat(ids).hasSize(2);
		assertThat(ids.get(0)).isEqualTo(errands);
		assertThat(mvc.get().uri("/api/labels")).bodyJson()
			.extractingPath("$[*].name")
			.asArray()
			.containsExactly("errands", "urgent");
	}

	@Test
	void requireExistingRejectsUnknownIds() throws Exception {
		UUID errands = UUID.fromString(idOf(post("errands")));

		labelApi.requireExisting(Set.of(errands));
		assertThatThrownBy(() -> labelApi.requireExisting(Set.of(errands, UUID.fromString(MISSING_ID))))
			.isInstanceOf(LabelNotFoundException.class);
	}

	private MvcTestResult post(String name) {
		return mvc.post()
			.uri("/api/labels")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"" + name + "\"}")
			.exchange();
	}

	private MvcTestResult put(String id, String name) {
		return mvc.put()
			.uri("/api/labels/{id}", id)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"" + name + "\"}")
			.exchange();
	}

	private static String idOf(MvcTestResult result) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

}
