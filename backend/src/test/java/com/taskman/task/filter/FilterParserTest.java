package com.taskman.task.filter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Built test-first: add a row here before changing the parser. Columns are separated
 * by ';' because queries contain '|'. Positions in messages are 1-based.
 */
class FilterParserTest {

	private final FilterParser parser = new FilterParser();

	@ParameterizedTest(name = "[{index}] {0}")
	@CsvSource(delimiter = ';', ignoreLeadingAndTrailingWhitespace = true, textBlock = """
			# query                              ; tree

			# Terms; keywords ignore case, names keep it
			today                                ; Today
			TODAY                                ; Today
			tomorrow                             ; Tomorrow
			overdue                              ; Overdue
			no date                              ; NoDate
			No   Date                            ; NoDate
			recurring                            ; Recurring
			p1                                   ; Priority(1)
			P4                                   ; Priority(4)
			#work                                ; Project(work)
			#Q4-planning                         ; Project(Q4-planning)
			@errands                             ; Label(errands)

			# Operators, with or without spaces
			today | overdue                      ; Or(Today, Overdue)
			today|overdue                        ; Or(Today, Overdue)
			p1 & #work                           ; And(Priority(1), Project(work))
			!no date                             ; Not(NoDate)

			# & binds tighter than |, and ! tighter than &
			today | overdue & #work              ; Or(Today, And(Overdue, Project(work)))
			(today | overdue) & #work            ; And(Or(Today, Overdue), Project(work))
			!no date & p1                        ; And(Not(NoDate), Priority(1))
			!(today | overdue)                   ; Not(Or(Today, Overdue))
			#work & !@waiting                    ; And(Project(work), Not(Label(waiting)))

			# Chains group to the left
			today | tomorrow | overdue           ; Or(Or(Today, Tomorrow), Overdue)
			p1 & #work & @errands                ; And(And(Priority(1), Project(work)), Label(errands))

			# Nesting and whitespace
			!!today                              ; Not(Not(Today))
			((today))                            ; Today
			'  today  '                          ; Today
			""")
	void parses(String query, String tree) {
		assertThat(parser.parse(query)).hasToString(tree);
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@CsvSource(delimiter = ';', ignoreLeadingAndTrailingWhitespace = true, textBlock = """
			# query                              ; message
			''                                   ; Filter is empty
			'   '                                ; Filter is empty
			today &                              ; Expected a filter at position 8 but the query ended
			& today                              ; Expected a filter at position 1 but found '&'
			today | | overdue                    ; Expected a filter at position 9 but found '|'
			()                                   ; Expected a filter at position 2 but found ')'
			(today                               ; Expected ')' at position 7 but the query ended
			today )                              ; Unexpected ')' at position 7
			today today                          ; Expected & or | at position 7 but found 'today'
			tomorow                              ; Unknown filter 'tomorow' at position 1
			p5                                   ; Unknown filter 'p5' at position 1
			no                                   ; Unknown filter 'no' at position 1
			today & no dates                     ; Unknown filter 'no' at position 9
			#                                    ; Expected a project name after '#' at position 1
			@                                    ; Expected a label name after '@' at position 1
			""")
	void rejects(String query, String message) {
		assertThatThrownBy(() -> parser.parse(query)).isInstanceOf(FilterSyntaxException.class).hasMessage(message);
	}

}
