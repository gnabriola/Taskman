package com.taskman.task;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

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
class QuickAddApiTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	private String homeId;

	@BeforeEach
	void setUp() throws Exception {
		databaseCleaner.clean();
		homeId = createProject("Home");
	}

	@Test
	void createsTaskFromText() throws Exception {
		MvcTestResult result = quickAdd("pay rent 2026-12-01 18:00 p1", "Europe/Berlin");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		assertThat(result.getResponse().getHeader("Location")).endsWith("/api/tasks/" + id);
		assertThat(result).bodyJson().isLenientlyEqualTo("""
				{"projectId": "%s", "title": "pay rent", "dueDate": "2026-12-01", "dueTime": "18:00:00",
				 "priority": 1, "completed": false}
				""".formatted(homeId));
	}

	@Test
	void createsRecurringTask() {
		assertThat(quickAdd("pay rent every 1st p1", "UTC")).hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.isLenientlyEqualTo("""
					{"title": "pay rent", "recurrence": "FREQ=MONTHLY;BYMONTHDAY=1", "recurrenceFromCompletion": false,
					 "recurrenceLabel": "every 1st", "priority": 1}
					""");
	}

	@Test
	void attachesLabelsAndCreatesMissingOnes() throws Exception {
		MvcTestResult existing = mvc.post()
			.uri("/api/labels")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"errands\"}")
			.exchange();
		String errands = JsonPath.read(existing.getResponse().getContentAsString(), "$.id");

		MvcTestResult result = quickAdd("buy milk @Errands @urgent", "UTC");

		assertThat(result).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.title").isEqualTo("buy milk");
		List<String> labelIds = JsonPath.read(result.getResponse().getContentAsString(), "$.labelIds");
		assertThat(labelIds).hasSize(2).contains(errands);
		assertThat(mvc.get().uri("/api/labels")).bodyJson()
			.extractingPath("$[*].name")
			.asArray()
			.containsExactly("errands", "urgent");
	}

	@Test
	void putsTaskIntoNamedProject() throws Exception {
		String workId = createProject("Work");

		assertThat(quickAdd("write report #work", "UTC")).hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.extractingPath("$.projectId")
			.isEqualTo(workId);
	}

	@Test
	void interpretsTodayInTheGivenTimeZone() {
		// UTC+14 and UTC-11 are always on different calendar days
		for (String zone : new String[] { "Pacific/Kiritimati", "Pacific/Pago_Pago" }) {
			String today = LocalDate.now(ZoneId.of(zone)).toString();
			assertThat(quickAdd("standup today", zone)).bodyJson().extractingPath("$.dueDate").isEqualTo(today);
		}
	}

	@Test
	void rejectsUnknownProjectWith422() {
		assertThat(quickAdd("pay rent #hoem", "UTC")).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("No project named 'hoem'");
	}

	@Test
	void rejectsTextWithoutTitleWith422() {
		assertThat(quickAdd("tomorrow 5pm", "UTC")).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
	}

	@Test
	void rejectsInvalidTimeZone() {
		assertThat(quickAdd("pay rent", "Mars/Olympus")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("timeZone");
	}

	@Test
	void rejectsMissingDefaultProject() {
		assertThat(mvc.post().uri("/api/tasks/quick-add").contentType(MediaType.APPLICATION_JSON).content("""
				{"text": "pay rent", "defaultProjectId": "0199c3a0-0000-7000-8000-000000000000", "timeZone": "UTC"}
				""")).hasStatus(HttpStatus.NOT_FOUND);
	}

	private MvcTestResult quickAdd(String text, String timeZone) {
		return mvc.post().uri("/api/tasks/quick-add").contentType(MediaType.APPLICATION_JSON).content("""
				{"text": "%s", "defaultProjectId": "%s", "timeZone": "%s"}
				""".formatted(text, homeId, timeZone)).exchange();
	}

	private String createProject(String name) throws Exception {
		MvcTestResult result = mvc.post()
			.uri("/api/projects")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"" + name + "\"}")
			.exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

}
