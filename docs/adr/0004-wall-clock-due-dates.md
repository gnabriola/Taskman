# ADR-004: Due dates are wall-clock date and time with no zone

- **Status:** Accepted
- **Date:** 2026-10-09

## Context

Tasks get a due date and, optionally, a due time ("pay rent tomorrow 5pm"). We have to
decide how to store them. Two models are common:

1. **Instant:** a single point in time (`timestamptz` / `Instant`), e.g.
   `2026-10-09T15:00:00Z`.
2. **Wall clock:** a calendar date plus an optional local time, with no zone
   (`date` + `time` / `LocalDate` + `LocalTime`), e.g. `2026-10-09` and `17:00`.

A due date in a personal task manager describes *the user's day*, not a moment on a
global timeline. "Pay rent on the 9th" stays on the 9th when the user flies to another
continent, and "gym 6pm" means 6pm wherever they are. Many tasks also have a date but no
time, which an instant cannot express without inventing one (midnight in some zone).

There are no user accounts yet (roadmap step 5), so the server cannot know a user's
time zone on its own.

## Decision

- Store `due_date date` and `due_time time` on `task`, both nullable, with a check
  constraint that `due_time` requires `due_date` (migration V3).
- Map them to `LocalDate` and `LocalTime`. Neither the database nor the domain model
  holds a zone.
- Interpret relative input ("today", "5pm") in the **user's** time zone. Until users
  exist, the client sends its IANA zone with each quick-add request (`timeZone`), and
  the server computes "now" as `LocalDateTime.now(clock.withZone(zone))`.
- Decide what is "today" or "overdue" in the client, against the browser's local date
  and time.

## Consequences

- **Good:** "5pm" means 5pm in whatever zone the user is in. Date-only tasks are
  represented exactly, with no fake midnight. The model matches what the user typed and
  is easy to explain.
- **Good:** no zone-conversion bugs on the server. Daylight-saving changes cannot move a
  task to another day.
- **Bad:** a due time is not a moment, so it cannot be compared across users. Shared
  projects (a 9:00 meeting for people in Berlin and New York) would need an explicit
  zone per task.
- **Bad:** reminders and notifications (roadmap step 7) need an instant. They will have
  to combine the wall-clock value with the user's zone, stored on the user once step 5
  adds users. A user who travels will get reminders in their current zone, which is
  usually what they want.
- **Follow-up:** once users exist, the zone moves from each request to the user profile,
  and server-side "today/overdue" queries (filters in step 4) need the user's zone too.

## Alternatives considered

- **`timestamptz` for everything:** simple to compare and sort, and ready for reminders.
  Rejected because date-only tasks need an invented time, and the meaning of a task
  changes when the user changes zone.
- **Wall clock plus a stored zone per task:** the most expressive model, and possibly
  where shared projects end up. Rejected for now as complexity without a current use
  case. It can be added later as a nullable `due_zone` column.
