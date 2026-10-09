# ADR-005: A hand-written quick-add parser instead of a library

- **Status:** Accepted
- **Date:** 2026-10-09

## Context

Quick-add turns text like `pay rent tomorrow 5pm #home p1` into a task: a title, a due
date and time, a project and a priority. We need a parser for the date part
("tomorrow", "friday", "in 3 days", "oct 31", "5pm") and for the Todoist-style markers
`#project` and `p1`–`p4`.

Options:

1. A general natural-language date library (e.g. Natty, or Hawking).
2. A hand-written parser for a deliberately small grammar.

CLAUDE.md requires approval for new libraries, and the parser is meant to be built
test-first so that every supported phrase is documented by a test case.

## Decision

Write the parser by hand in `com.taskman.task.quickadd`, as plain Java with no Spring and
no dependencies:

- Split the text into words. Walk left to right, and at each word try a list of small
  rules (relative day, `next week` / `next <weekday>`, weekday, `in N days/weeks`,
  ISO date, month-day, day-month, 12-hour and 24-hour times, `#project`, `p1`–`p4`).
  Each rule either claims one or more words or does nothing.
- Use only the first date and the first time. Words no rule claimed form the title.
- A filler word ("on", "at") is removed only directly before a date or time, so
  "on hold" and "look at it" stay intact.
- "Now" is passed in (`parse(text, LocalDateTime now)`), so the parser is a pure function
  and every case is deterministic.
- Behaviour is specified by the table in `QuickAddParserTest`. New phrases are added to
  the table first, then implemented.

## Consequences

- **Good:** the supported grammar is exactly what the table says: no surprises, and it's
  easy to explain in an interview. The rule list is a simple strategy pattern; adding a
  phrase means adding a rule and its rows.
- **Good:** no dependency to vet or keep updated. The available libraries are not
  actively maintained or are heavyweight, and none understands `#project` or `p1`.
- **Good:** Todoist-specific semantics (a weekday always means the next one *after*
  today; a time on its own means its next occurrence) are ours to define.
- **Bad:** coverage is narrow. English only, no "the day after tomorrow", no "end of
  month", and no `dd/mm` vs `mm/dd` handling. Each new phrase is our own work.
- **Bad:** some ambiguity is unavoidable. "sat", "sun", "may", "mar" are also ordinary
  words, and `#42` reads as a project. There is no escape syntax yet.
- **Follow-up:** recurring phrases ("every monday", step 3) extend the same rule list,
  rather than starting a second parser.

## Alternatives considered

- **Natty / similar NLP date libraries:** broader coverage, but larger, less
  predictable output that we would have to wrap and test anyway, and unmaintained
  projects. Rejected.
- **One large regular expression:** compact, but hard to read, test and extend.
  Rejected in favour of small named rules.
- **Parsing in the frontend:** would allow live highlighting while typing, but "now"
  and the project lookup belong on the server, and one implementation is easier to
  test. Live highlighting can call the server later if needed.
