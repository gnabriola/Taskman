package com.taskman;

import com.taskman.user.TestUsers;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Empties every application table between tests, then re-creates the default test
 * user. Tests commit for real instead of rolling back, so constraint violations and
 * lazy-loading bugs are not hidden.
 */
@TestComponent
public class DatabaseCleaner {

	private final JdbcTemplate jdbc;

	DatabaseCleaner(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public void clean() {
		String tables = jdbc.queryForObject("""
				SELECT string_agg(format('%I', tablename), ', ')
				FROM pg_tables
				WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
				""", String.class);
		if (tables != null) {
			jdbc.execute("TRUNCATE " + tables + " CASCADE");
		}
		// The password is irrelevant: tests authenticate this user without logging in.
		jdbc.update("""
				INSERT INTO app_user (id, email, display_name, password_hash, created_at, updated_at)
				VALUES (?, ?, ?, '{noop}unused', now(), now())
				""", TestUsers.ID, TestUsers.EMAIL, TestUsers.DISPLAY_NAME);
	}

}
