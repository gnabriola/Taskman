package com.taskman.task.quickadd;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The parser is built test-first: add a row here before changing the parser.
 * "Now" is Wednesday 2026-10-07 10:00. Empty cells mean "not set".
 */
class QuickAddParserTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 10, 0);

	private final QuickAddParser parser = new QuickAddParser();

	@ParameterizedTest(name = "[{index}] {0}")
	@CsvSource(delimiter = '|', ignoreLeadingAndTrailingWhitespace = true, textBlock = """
			# input                            | title                | due date   | due time | project | priority

			# The full example
			pay rent tomorrow 5pm #home p1     | pay rent             | 2026-10-08 | 17:00    | home    | 1

			# Plain text
			buy milk                           | buy milk             |            |          |         |
			'  buy   milk  '                   | buy milk             |            |          |         |

			# Relative days, any case
			buy milk today                     | buy milk             | 2026-10-07 |          |         |
			buy milk tod                       | buy milk             | 2026-10-07 |          |         |
			call mom tomorrow                  | call mom             | 2026-10-08 |          |         |
			call mom tmr                       | call mom             | 2026-10-08 |          |         |
			call mom tmrw                      | call mom             | 2026-10-08 |          |         |
			Tomorrow cleanup                   | cleanup              | 2026-10-08 |          |         |

			# Weekdays: always the next one strictly after today
			dentist friday                     | dentist              | 2026-10-09 |          |         |
			standup mon                        | standup              | 2026-10-12 |          |         |
			review wednesday                   | review               | 2026-10-14 |          |         |
			review wed                         | review               | 2026-10-14 |          |         |
			standup next monday                | standup              | 2026-10-12 |          |         |
			meeting on friday                  | meeting              | 2026-10-09 |          |         |

			# Relative periods
			report next week                   | report               | 2026-10-12 |          |         |
			renew passport in 3 days           | renew passport       | 2026-10-10 |          |         |
			water plants in 1 day              | water plants         | 2026-10-08 |          |         |
			book flights in 2 weeks            | book flights         | 2026-10-21 |          |         |

			# Explicit dates; a date already past this year means next year
			party oct 31                       | party                | 2026-10-31 |          |         |
			party 31 oct                       | party                | 2026-10-31 |          |         |
			party october 31                   | party                | 2026-10-31 |          |         |
			sync oct 7                         | sync                 | 2026-10-07 |          |         |
			tax return 15 mar                  | tax return           | 2027-03-15 |          |         |
			deploy 2026-12-01                  | deploy               | 2026-12-01 |          |         |
			plan feb 30                        | plan feb 30          |            |          |         |

			# Times; a time without a date means its next occurrence
			deploy 2026-12-01 18:00            | deploy               | 2026-12-01 | 18:00    |         |
			dentist friday 9:30am              | dentist              | 2026-10-09 | 09:30    |         |
			gym 6pm                            | gym                  | 2026-10-07 | 18:00    |         |
			gym 6 pm                           | gym                  | 2026-10-07 | 18:00    |         |
			coffee 8am                         | coffee               | 2026-10-08 | 08:00    |         |
			standup 10:00                      | standup              | 2026-10-08 | 10:00    |         |
			lunch at 12:30                     | lunch                | 2026-10-07 | 12:30    |         |
			lunch at 12pm                      | lunch                | 2026-10-07 | 12:00    |         |
			backup 12am                        | backup               | 2026-10-08 | 00:00    |         |
			coffee today 8am                   | coffee               | 2026-10-07 | 08:00    |         |
			call at 5pm tomorrow               | call                 | 2026-10-08 | 17:00    |         |
			alarm 13pm                         | alarm 13pm           |            |          |         |
			alarm 25:00                        | alarm 25:00          |            |          |         |

			# Projects: name as typed, lookup is the caller's job
			fix bug #taskman                   | fix bug              |            |          | taskman |
			#Work write report                 | write report         |            |          | Work    |
			price is 5 #                       | price is 5 #         |            |          |         |

			# Priorities
			pay rent p1                        | pay rent             |            |          |         | 1
			pay rent P4                        | pay rent             |            |          |         | 4
			buy p5 pencils                     | buy p5 pencils       |            |          |         |
			buy mp3 player                     | buy mp3 player       |            |          |         |

			# Words that only look like dates or times stay in the title
			read 1984                          | read 1984            |            |          |         |
			on hold                            | on hold              |            |          |         |
			look at it                         | look at it           |            |          |         |
			in progress                        | in progress          |            |          |         |

			# Only the first date and time are used; the rest stays in the title
			move friday to monday              | move to monday       | 2026-10-09 |          |         |

			# "every" without a recurrence after it stays in the title
			fix every time                     | fix every time       |            |          |         |
			email everyone                     | email everyone       |            |          |         |
			retry every 0 days                 | retry every 0 days   |            |          |         |

			# Nothing left for the title: the parser reports an empty title
			tomorrow                           | ''                   | 2026-10-08 |          |         |
			""")
	void parses(String input, String title, LocalDate dueDate, LocalTime dueTime, String project, Integer priority) {
		ParsedTask parsed = parser.parse(input, NOW);

		assertThat(parsed).isEqualTo(new ParsedTask(title, dueDate, dueTime, project, priority, null));
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@CsvSource(delimiter = '|', ignoreLeadingAndTrailingWhitespace = true, textBlock = """
			# input                            | title        | first due  | time  | rule                                  | every!
			water plants every day             | water plants | 2026-10-07 |       | FREQ=DAILY                            | false
			standup every weekday 9:30am       | standup      | 2026-10-08 | 09:30 | FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR      | false
			clean every weekend                | clean        | 2026-10-10 |       | FREQ=WEEKLY;BYDAY=SA,SU               | false

			# Weekdays: today counts as the first occurrence
			team sync every wednesday          | team sync    | 2026-10-07 |       | FREQ=WEEKLY;BYDAY=WE                  | false
			review every friday                | review       | 2026-10-09 |       | FREQ=WEEKLY;BYDAY=FR                  | false
			gym every mon, wed, fri            | gym          | 2026-10-07 |       | FREQ=WEEKLY;BYDAY=MO,WE,FR            | false
			gym every mon,fri                  | gym          | 2026-10-09 |       | FREQ=WEEKLY;BYDAY=MO,FR               | false
			gym every tue and thu              | gym          | 2026-10-08 |       | FREQ=WEEKLY;BYDAY=TU,TH               | false
			payroll every other friday         | payroll      | 2026-10-09 |       | FREQ=WEEKLY;INTERVAL=2;BYDAY=FR       | false

			# Intervals; week, month and year keep today's weekday or date as anchor
			backup every 3 days                | backup       | 2026-10-07 |       | FREQ=DAILY;INTERVAL=3                 | false
			call every other day               | call         | 2026-10-07 |       | FREQ=DAILY;INTERVAL=2                 | false
			report every week                  | report       | 2026-10-07 |       | FREQ=WEEKLY;BYDAY=WE                  | false
			1:1 every other week               | 1:1          | 2026-10-07 |       | FREQ=WEEKLY;INTERVAL=2;BYDAY=WE       | false
			sprint every 2 weeks               | sprint       | 2026-10-07 |       | FREQ=WEEKLY;INTERVAL=2;BYDAY=WE       | false
			pay rent every month               | pay rent     | 2026-10-07 |       | FREQ=MONTHLY;BYMONTHDAY=7             | false
			review every 3 months              | review       | 2026-10-07 |       | FREQ=MONTHLY;INTERVAL=3;BYMONTHDAY=7  | false
			taxes every year                   | taxes        | 2026-10-07 |       | FREQ=YEARLY;BYMONTH=10;BYMONTHDAY=7   | false

			# Days of the month and of the year
			pay rent every 1st                 | pay rent     | 2026-11-01 |       | FREQ=MONTHLY;BYMONTHDAY=1             | false
			invoice every 31st                 | invoice      | 2026-10-31 |       | FREQ=MONTHLY;BYMONTHDAY=31            | false
			birthday every oct 31              | birthday     | 2026-10-31 |       | FREQ=YEARLY;BYMONTH=10;BYMONTHDAY=31  | false
			anniversary every 7 october        | anniversary  | 2026-10-07 |       | FREQ=YEARLY;BYMONTH=10;BYMONTHDAY=7   | false

			# With a time: today if the time is still ahead, otherwise the next occurrence
			meds every day 8pm                 | meds         | 2026-10-07 | 20:00 | FREQ=DAILY                            | false
			meds every day 8am                 | meds         | 2026-10-08 | 08:00 | FREQ=DAILY                            | false
			Every Monday at 9:00 standup       | standup      | 2026-10-12 | 09:00 | FREQ=WEEKLY;BYDAY=MO                  | false

			# every!: counted from the completion date, no fixed weekday or date
			haircut every! 4 weeks             | haircut      | 2026-10-07 |       | FREQ=WEEKLY;INTERVAL=4                | true
			call mom every! week               | call mom     | 2026-10-07 |       | FREQ=WEEKLY                           | true
			water cactus every! month          | water cactus | 2026-10-07 |       | FREQ=MONTHLY                          | true
			stretch every! day                 | stretch      | 2026-10-07 |       | FREQ=DAILY                            | true

			# Nothing left for the title
			every day                          | ''           | 2026-10-07 |       | FREQ=DAILY                            | false
			""")
	void parsesRecurrence(String input, String title, LocalDate firstDue, LocalTime dueTime, String rule,
			boolean fromCompletion) {
		ParsedTask parsed = parser.parse(input, NOW);

		assertThat(parsed.title()).isEqualTo(title);
		assertThat(parsed.dueDate()).isEqualTo(firstDue);
		assertThat(parsed.dueTime()).isEqualTo(dueTime);
		assertThat(parsed.recurrence()).isNotNull();
		assertThat(parsed.recurrence().rule()).isEqualTo(rule);
		assertThat(parsed.recurrence().fromCompletion()).isEqualTo(fromCompletion);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@CsvSource(delimiter = '|', ignoreLeadingAndTrailingWhitespace = true, textBlock = """
			# input                            | project | priority
			meds every day 8pm #home p1        | home    | 1
			""")
	void recurrenceCombinesWithProjectAndPriority(String input, String project, Integer priority) {
		ParsedTask parsed = parser.parse(input, NOW);

		assertThat(parsed.title()).isEqualTo("meds");
		assertThat(parsed.recurrence()).isNotNull();
		assertThat(parsed.projectName()).isEqualTo(project);
		assertThat(parsed.priority()).isEqualTo(priority);
	}

}
