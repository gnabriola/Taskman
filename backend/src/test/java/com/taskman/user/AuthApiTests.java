package com.taskman.user;

import com.taskman.DatabaseCleaner;
import com.taskman.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Real logins with real sessions, so this builds its own MockMvc without the default
 * test user that other integration tests run as.
 */
@IntegrationTest
class AuthApiTests {

	private static final String PASSWORD = "correct horse";

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	private MockMvcTester mvc;

	@BeforeEach
	void setUp() {
		databaseCleaner.clean();
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		mvc = MockMvcTester.create(mockMvc);
	}

	@Test
	void apiRequiresLogin() {
		assertThat(mvc.get().uri("/api/projects")).hasStatus(HttpStatus.UNAUTHORIZED)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("You need to log in");
	}

	@Test
	void registerLogsTheUserIn() {
		MvcTestResult result = register("  Ada@Example.com ", "Ada", PASSWORD);

		assertThat(result).hasStatus(HttpStatus.CREATED).bodyJson().isLenientlyEqualTo("""
				{"email": "ada@example.com", "displayName": "Ada"}
				""");
		assertThat(mvc.get().uri("/api/auth/me").session(sessionOf(result))).hasStatusOk()
			.bodyJson()
			.extractingPath("$.email")
			.isEqualTo("ada@example.com");
	}

	@Test
	void rejectsDuplicateEmailIgnoringCase() {
		register("ada@example.com", "Ada", PASSWORD);

		assertThat(register("ADA@example.com", "Other Ada", PASSWORD)).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void validatesRegistration() {
		assertThat(register("not-an-email", "Ada", PASSWORD)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(register("ada@example.com", "Ada", "short")).hasStatus(HttpStatus.BAD_REQUEST);
		// 37 characters, but 74 bytes in UTF-8: over BCrypt's 72-byte limit
		assertThat(register("ada@example.com", "Ada", "é".repeat(37))).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("password");
	}

	@Test
	void logsInWithCorrectPasswordAndIssuesANewSessionId() {
		register("ada@example.com", "Ada", PASSWORD);
		MockHttpSession before = new MockHttpSession();
		String idBefore = before.getId();

		MvcTestResult result = login("ADA@example.com", PASSWORD, before);

		assertThat(result).hasStatusOk().bodyJson().extractingPath("$.displayName").isEqualTo("Ada");
		// A new id prevents session fixation: an id planted before login is useless after it.
		assertThat(sessionOf(result).getId()).isNotEqualTo(idBefore);
		assertThat(mvc.get().uri("/api/auth/me").session(sessionOf(result))).hasStatusOk();
	}

	@Test
	void wrongPasswordAndUnknownEmailLookTheSame() {
		register("ada@example.com", "Ada", PASSWORD);

		for (MvcTestResult result : new MvcTestResult[] { login("ada@example.com", "wrong password", null),
				login("nobody@example.com", PASSWORD, null) }) {
			assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED)
				.bodyJson()
				.extractingPath("$.detail")
				.isEqualTo("Invalid email or password");
		}
	}

	@Test
	void logoutEndsTheSession() {
		MockHttpSession session = sessionOf(register("ada@example.com", "Ada", PASSWORD));

		assertThat(mvc.post().uri("/api/auth/logout").session(session).with(csrf()))
			.hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.get().uri("/api/auth/me").session(session)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void stateChangingRequestsNeedACsrfToken() {
		assertThat(mvc.post()
			.uri("/api/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email": "ada@example.com", "password": "correct horse"}
					""")).hasStatus(HttpStatus.FORBIDDEN).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	private MvcTestResult register(String email, String displayName, String password) {
		return mvc.post()
			.uri("/api/auth/register")
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email": "%s", "displayName": "%s", "password": "%s"}
					""".formatted(email, displayName, password))
			.exchange();
	}

	private MvcTestResult login(String email, String password, MockHttpSession session) {
		var request = mvc.post()
			.uri("/api/auth/login")
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email": "%s", "password": "%s"}
					""".formatted(email, password));
		if (session != null) {
			request.session(session);
		}
		return request.exchange();
	}

	private static MockHttpSession sessionOf(MvcTestResult result) {
		return (MockHttpSession) result.getRequest().getSession(false);
	}

}
