package com.taskman.task.recurrence;

import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Built test-first: add a row before changing {@link Recurrence}.
 * Weekdays for reference: 2026-10-07 is a Wednesday, 2026-10-12 a Monday.
 */
class RecurrenceTest {

	@ParameterizedTest(name = "[{index}] {0} after {1} -> {2}")
	@CsvSource(delimiter = '|', ignoreLeadingAndTrailingWhitespace = true, textBlock = """
			# rule                                    | from       | next
			FREQ=DAILY                                | 2026-10-07 | 2026-10-08
			FREQ=DAILY;INTERVAL=3                     | 2026-10-07 | 2026-10-10
			FREQ=WEEKLY                               | 2026-10-07 | 2026-10-14
			FREQ=WEEKLY;BYDAY=MO,WE,FR                | 2026-10-07 | 2026-10-09
			FREQ=WEEKLY;BYDAY=MO,WE,FR                | 2026-10-09 | 2026-10-12
			FREQ=WEEKLY;BYDAY=WE                      | 2026-10-07 | 2026-10-14
			FREQ=WEEKLY;BYDAY=SA,SU                   | 2026-10-11 | 2026-10-17
			FREQ=WEEKLY;INTERVAL=2;BYDAY=MO           | 2026-10-12 | 2026-10-26
			FREQ=WEEKLY;INTERVAL=2;BYDAY=WE           | 2026-10-06 | 2026-10-07
			FREQ=MONTHLY                              | 2026-01-31 | 2026-02-28
			FREQ=MONTHLY;BYMONTHDAY=7                 | 2026-10-07 | 2026-11-07
			FREQ=MONTHLY;BYMONTHDAY=15                | 2026-10-07 | 2026-10-15
			FREQ=MONTHLY;BYMONTHDAY=31                | 2026-10-31 | 2026-11-30
			FREQ=MONTHLY;BYMONTHDAY=31                | 2026-11-30 | 2026-12-31
			FREQ=MONTHLY;BYMONTHDAY=31                | 2027-01-31 | 2027-02-28
			FREQ=MONTHLY;INTERVAL=3;BYMONTHDAY=7      | 2026-10-07 | 2027-01-07
			FREQ=YEARLY                               | 2026-10-07 | 2027-10-07
			FREQ=YEARLY;BYMONTH=10;BYMONTHDAY=31      | 2026-10-07 | 2026-10-31
			FREQ=YEARLY;BYMONTH=10;BYMONTHDAY=31      | 2026-10-31 | 2027-10-31
			FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=29       | 2028-02-29 | 2029-02-28
			FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=29       | 2031-03-01 | 2032-02-29
			""")
	void nextAfter(String rule, LocalDate from, LocalDate next) {
		assertThat(Recurrence.parse(rule, false).nextAfter(from)).isEqualTo(next);
	}

	@ParameterizedTest(name = "[{index}] {0} every!={1}, due {2}, completed {3} -> {4}")
	@CsvSource(delimiter = '|', ignoreLeadingAndTrailingWhitespace = true, textBlock = """
			# rule                         | every! | due        | today      | next due
			# From the due date
			FREQ=DAILY                     | false  | 2026-10-07 | 2026-10-07 | 2026-10-08
			# Overdue: catch up to after today instead of staying in the past
			FREQ=DAILY                     | false  | 2026-10-02 | 2026-10-07 | 2026-10-08
			FREQ=WEEKLY;BYDAY=MO           | false  | 2026-09-28 | 2026-10-07 | 2026-10-12
			# Completed early: the next occurrence after the due date
			FREQ=DAILY                     | false  | 2026-10-09 | 2026-10-07 | 2026-10-10
			FREQ=MONTHLY;BYMONTHDAY=31     | false  | 2026-10-31 | 2026-10-07 | 2026-11-30
			# every!: counted from the completion date, wherever the due date was
			FREQ=DAILY;INTERVAL=3          | true   | 2026-10-01 | 2026-10-07 | 2026-10-10
			FREQ=WEEKLY;INTERVAL=4         | true   | 2026-12-01 | 2026-10-07 | 2026-11-04
			FREQ=MONTHLY                   | true   | 2026-09-30 | 2026-10-07 | 2026-11-07
			""")
	void nextDue(String rule, boolean fromCompletion, LocalDate due, LocalDate today, LocalDate next) {
		assertThat(Recurrence.parse(rule, fromCompletion).nextDue(due, today)).isEqualTo(next);
	}

	@ParameterizedTest(name = "[{index}] {0} every!={1} from {2} -> {3}")
	@CsvSource(delimiter = '|', ignoreLeadingAndTrailingWhitespace = true, textBlock = """
			# rule                                  | every! | date       | first occurrence
			FREQ=DAILY;INTERVAL=3                   | false  | 2026-10-07 | 2026-10-07
			FREQ=WEEKLY;BYDAY=WE                    | false  | 2026-10-07 | 2026-10-07
			FREQ=WEEKLY;BYDAY=FR                    | false  | 2026-10-07 | 2026-10-09
			FREQ=WEEKLY;BYDAY=SA,SU                 | false  | 2026-10-07 | 2026-10-10
			FREQ=MONTHLY;BYMONTHDAY=1               | false  | 2026-10-07 | 2026-11-01
			FREQ=MONTHLY;BYMONTHDAY=7               | false  | 2026-10-07 | 2026-10-07
			FREQ=YEARLY;BYMONTH=10;BYMONTHDAY=31    | false  | 2026-10-07 | 2026-10-31
			FREQ=WEEKLY;INTERVAL=4                  | true   | 2026-10-07 | 2026-10-07
			""")
	void firstOnOrAfter(String rule, boolean fromCompletion, LocalDate date, LocalDate first) {
		assertThat(Recurrence.parse(rule, fromCompletion).firstOnOrAfter(date)).isEqualTo(first);
	}

	@ParameterizedTest(name = "[{index}] {0} -> {1}")
	@CsvSource(delimiter = '|', ignoreLeadingAndTrailingWhitespace = true, textBlock = """
			# input                                  | normalized rule
			FREQ=DAILY                               | FREQ=DAILY
			freq=daily;interval=1                    | FREQ=DAILY
			FREQ=WEEKLY;BYDAY=FR,MO                  | FREQ=WEEKLY;BYDAY=MO,FR
			BYDAY=MO;FREQ=WEEKLY;INTERVAL=2          | FREQ=WEEKLY;INTERVAL=2;BYDAY=MO
			FREQ=YEARLY;BYMONTHDAY=31;BYMONTH=10     | FREQ=YEARLY;BYMONTH=10;BYMONTHDAY=31
			""")
	void normalizesRule(String input, String normalized) {
		assertThat(Recurrence.parse(input, false).rule()).isEqualTo(normalized);
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@ValueSource(strings = { "", "FREQ=HOURLY", "INTERVAL=2", "FREQ=DAILY;INTERVAL=0", "FREQ=DAILY;INTERVAL=x",
			"FREQ=DAILY;BYDAY=MO", "FREQ=WEEKLY;BYDAY=XX", "FREQ=WEEKLY;BYDAY=", "FREQ=MONTHLY;BYMONTHDAY=32",
			"FREQ=MONTHLY;BYMONTH=2", "FREQ=YEARLY;BYMONTH=2", "FREQ=YEARLY;BYMONTH=13;BYMONTHDAY=1",
			"FREQ=DAILY;COUNT=3", "FREQ=DAILY;FREQ=WEEKLY", "FREQ", "nonsense" })
	void rejectsInvalidRule(String rule) {
		assertThatThrownBy(() -> Recurrence.parse(rule, false)).isInstanceOf(InvalidRecurrenceException.class);
	}

	@ParameterizedTest(name = "[{index}] {0} every!={1} -> {2}")
	@CsvSource(delimiter = '|', ignoreLeadingAndTrailingWhitespace = true, textBlock = """
			# rule                                 | every! | label
			FREQ=DAILY                             | false  | every day
			FREQ=DAILY;INTERVAL=3                  | false  | every 3 days
			FREQ=WEEKLY                            | false  | every week
			FREQ=WEEKLY;BYDAY=WE                   | false  | every Wednesday
			FREQ=WEEKLY;BYDAY=MO,WE,FR             | false  | every Mon, Wed, Fri
			FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR       | false  | every weekday
			FREQ=WEEKLY;BYDAY=SA,SU                | false  | every weekend
			FREQ=WEEKLY;INTERVAL=2                 | false  | every 2 weeks
			FREQ=WEEKLY;INTERVAL=2;BYDAY=FR        | false  | every other Friday
			FREQ=WEEKLY;INTERVAL=3;BYDAY=MO,FR     | false  | every 3 weeks on Mon, Fri
			FREQ=MONTHLY                           | false  | every month
			FREQ=MONTHLY;BYMONTHDAY=1              | false  | every 1st
			FREQ=MONTHLY;BYMONTHDAY=22             | false  | every 22nd
			FREQ=MONTHLY;BYMONTHDAY=13             | false  | every 13th
			FREQ=MONTHLY;INTERVAL=3;BYMONTHDAY=7   | false  | every 3 months on the 7th
			FREQ=YEARLY                            | false  | every year
			FREQ=YEARLY;BYMONTH=10;BYMONTHDAY=31   | false  | every Oct 31
			FREQ=WEEKLY;INTERVAL=4                 | true   | every! 4 weeks
			FREQ=MONTHLY                           | true   | every! month
			""")
	void label(String rule, boolean fromCompletion, String label) {
		assertThat(Recurrence.parse(rule, fromCompletion).label()).isEqualTo(label);
	}

}
