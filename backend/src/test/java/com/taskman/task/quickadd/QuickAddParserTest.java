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

			# Nothing left for the title: the parser reports an empty title
			tomorrow                           | ''                   | 2026-10-08 |          |         |
			""")
	void parses(String input, String title, LocalDate dueDate, LocalTime dueTime, String project, Integer priority) {
		ParsedTask parsed = parser.parse(input, NOW);

		assertThat(parsed).isEqualTo(new ParsedTask(title, dueDate, dueTime, project, priority));
	}

}
