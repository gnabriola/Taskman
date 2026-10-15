package com.taskman.user;

import com.taskman.DatabaseCleaner;
import com.taskman.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * The CSRF flow exactly as the Angular app does it: read the XSRF-TOKEN cookie from
 * any response, send it back as the X-XSRF-TOKEN header.
 * <p>
 * Runs in its own Spring context (the property only makes the context unique). Spring
 * Security's {@code csrf()} test helper, used everywhere else, permanently swaps the
 * shared CsrfFilter's cookie repository for a session-based test one, after which no
 * cookie would ever be written in that context.
 */
@IntegrationTest
@TestPropertySource(properties = "taskman.test.context=csrf-cookie-flow")
class CsrfCookieFlowTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	@Test
	void tokenFromCookieIsAcceptedAsHeader() {
		databaseCleaner.clean();
		MockMvcTester mvc = MockMvcTester
			.create(MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build());

		Cookie xsrf = mvc.get().uri("/api/auth/me").exchange().getResponse().getCookie("XSRF-TOKEN");
		assertThat(xsrf).as("XSRF-TOKEN cookie on an anonymous 401").isNotNull();
		assertThat(xsrf.isHttpOnly()).isFalse();

		assertThat(mvc.post()
			.uri("/api/auth/register")
			.cookie(xsrf)
			.header("X-XSRF-TOKEN", xsrf.getValue())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email": "ada@example.com", "displayName": "Ada", "password": "correct horse"}
					""")).hasStatus(HttpStatus.CREATED);

		assertThat(mvc.post()
			.uri("/api/auth/register")
			.cookie(xsrf)
			.header("X-XSRF-TOKEN", "forged")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email": "eve@example.com", "displayName": "Eve", "password": "correct horse"}
					""")).hasStatus(HttpStatus.FORBIDDEN);
	}

}
