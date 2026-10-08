package com.taskman;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Empties every application table between tests. Tests commit for real instead of
 * rolling back, so constraint violations and lazy-loading bugs are not hidden.
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
	}

}
