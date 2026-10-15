package com.taskman.user;

import java.util.UUID;

/**
 * The user every integration test runs as by default. {@code DatabaseCleaner} inserts a
 * matching row after each clean-up.
 */
public final class TestUsers {

	public static final UUID ID = UUID.fromString("0199c3a0-0000-7000-8000-00000000aaaa");

	public static final String EMAIL = "test@taskman.dev";

	public static final String DISPLAY_NAME = "Test User";

	private TestUsers() {
	}

	public static UserPrincipal principal() {
		return principal(ID, EMAIL, DISPLAY_NAME);
	}

	public static UserPrincipal principal(UUID id, String email, String displayName) {
		return new UserPrincipal(id, email, displayName, null);
	}

}
