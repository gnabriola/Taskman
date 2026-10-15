package com.taskman;

import com.taskman.user.TestUsers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Every MockMvc request runs as {@link TestUsers#principal()} with a valid CSRF token,
 * so feature tests do not repeat login boilerplate. Tests about authentication itself
 * override this per request (e.g. {@code .with(anonymous())}).
 */
@TestConfiguration(proxyBeanMethods = false)
class AuthenticatedMockMvcConfiguration {

	@Bean
	MockMvcBuilderCustomizer authenticatedByDefault() {
		return builder -> builder.defaultRequest(get("/").with(user(TestUsers.principal())).with(csrf()));
	}

}
