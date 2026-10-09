package com.taskman.task;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;


import com.jayway.jsonpath.JsonPath;
import com.taskman.DatabaseCleaner;
import com.taskman.IntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs filter queries against a fixed set of tasks (seeded once). Dates are relative
 * to today in UTC, the zone the queries use.
 */
@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FilterApiTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	@BeforeAll
	void seed() throws Exception {
		databaseCleaner.clean();
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		String home = create("/api/projects", "{\"name\": \"Home\"}");
		String work = create("/api/projects", "{\"name\": \"Work\"}");
		String errands = create("/api/labels", "{\"name\": \"errands\"}");

		task(home, """
				{"title": "dueToday", "dueDate": "%s", "priority": 1}""".formatted(today));
		task(work, """
				{"title": "dueTomorrow", "dueDate": "%s", "labelIds": ["%s"]}""".formatted(today.plusDays(1), errands));
		task(work, """
				{"title": "late", "dueDate": "%s", "priority": 1}""".formatted(today.minusDays(3)));
		task(home, """
				{"title": "undated", "labelIds": ["%s"]}""".formatted(errands));
		task(home, """
				{"title": "repeating", "dueDate": "%s", "recurrence": "FREQ=DAILY"}""".formatted(today.plusDays(10)));
		String done = task(home, """
				{"title": "finished", "dueDate": "%s"}""".formatted(today));
		assertThat(mvc.post().uri("/api/tasks/{id}/complete", done)).hasStatusOk();
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@CsvSource(delimiter = ';', ignoreLeadingAndTrailingWhitespace = true, textBlock = """
			# query                      ; expected titles, in order (completed tasks never match)
			today                        ; dueToday
			tomorrow                     ; dueTomorrow
			overdue                      ; late
			no date                      ; undated
			recurring                    ; repeating
			p1                           ; late dueToday
			#work                        ; late dueTomorrow
			#WORK                        ; late dueTomorrow
			@errands                     ; dueTomorrow undated
			today | overdue              ; late dueToday

			# Precedence: & binds tighter than |
			#work | today & p1           ; late dueToday dueTomorrow
			(#work | today) & p1         ; late dueToday

			# Negation keeps tasks without a date or label (no SQL NULL surprises)
			!today                       ; late dueTomorrow repeating undated
			!@errands                    ; late dueToday repeating
			!no date                     ; late dueToday dueTomorrow repeating
			""")
	void filters(String query, String expectedTitles) {
		assertThat(filter(query, "UTC")).hasStatusOk()
			.bodyJson()
			.extractingPath("$[*].title")
			.asArray()
			.containsExactlyElementsOf(Arrays.asList(expectedTitles.split(" ")));
	}

	@Test
	void reportsSyntaxErrorsWithPosition() {
		assertThat(filter("today &", "UTC")).hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Expected a filter at position 8 but the query ended");
	}

	@Test
	void rejectsUnknownProjectAndLabelWith422() {
		assertThat(filter("#nope", "UTC")).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("No project named 'nope'");
		assertThat(filter("@nope", "UTC")).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("No label named 'nope'");
	}

	@Test
	void rejectsInvalidTimeZone() {
		assertThat(filter("today", "Mars/Olympus")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void requiresAFilter() {
		assertThat(mvc.get().uri("/api/tasks")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	private MvcTestResult filter(String query, String timeZone) {
		return mvc.get().uri("/api/tasks").param("filter", query).param("timeZone", timeZone).exchange();
	}

	private String create(String uri, String json) throws Exception {
		MvcTestResult result = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String task(String projectId, String json) throws Exception {
		return create("/api/projects/" + projectId + "/tasks", json);
	}


}
