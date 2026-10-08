# Taskman — a Todoist-style task manager

Learning and portfolio project. The goal is a polished task manager that starts as a
**modular monolith** and is later split into **microservices**. I'm a Java/Spring
developer preparing for internship interviews, so I must be able to explain every
design decision in this codebase.

## Stack

- **Backend:** Java (latest LTS), Spring Boot 3+, Spring Data JPA, Spring Security
- **Database:** PostgreSQL, schema managed with Flyway
- **Frontend:** Angular (latest stable), TypeScript in strict mode
- **Infra:** Docker Compose for local development
- **Later:** Redis (caching), Kafka or RabbitMQ (events), Keycloak (auth), Prometheus + Grafana

Do not add new libraries or infrastructure without asking me first.

## Repository layout

```
backend/     Spring Boot application
frontend/    Angular application
infra/       docker-compose files and configs
docs/        architecture notes and decision records (docs/adr/)
```

## Backend conventions

- **Package by feature, not by layer:** `task`, `project`, `label`, `user`, `notification`.
  Each feature keeps its controller, service, repository, entities and DTOs together.
- **Module boundaries matter:** modules talk to each other only through public service
  interfaces or application events, never through another module's repositories or
  entities. This is what makes splitting into microservices possible later.
  The `notification` module must communicate with the rest **only via events**.
- Controllers return DTOs (Java records), never JPA entities.
- Validate input with Bean Validation; handle errors centrally with `@RestControllerAdvice`
  and return RFC 9457 problem details.
- Every schema change is a new Flyway migration. Never edit an existing migration.
- Use constructor injection. No field injection.

## Frontend conventions

- Standalone components, signals for state, feature-based folders mirroring the backend.
- Typed HTTP client services per feature; no `any`.
- Keyboard-first UX is a goal: every main action should have a shortcut.

## Testing

- Write or update tests with every feature. A feature isn't done until tests pass.
- Unit tests: JUnit 5 + AssertJ + Mockito.
- Integration tests: `@SpringBootTest` with **Testcontainers** (real PostgreSQL, no H2).
- The natural-language date parser is built **test-first**: add cases to its test
  table before changing the implementation.
- Run the backend tests with `./mvnw test` from `backend/` before saying a task is done.

## How to work with me

- For anything larger than a small fix, **propose a plan first** and wait for my OK.
- Prefer small, focused changes. One feature per branch and commit.
- When you make a non-obvious design choice, explain it in 2–3 sentences and suggest
  whether it deserves an ADR in `docs/adr/`.
- After finishing a feature, list the key concepts it used and 2–3 interview questions
  I should be able to answer about it.
- If my request is ambiguous or conflicts with these rules, ask instead of guessing.

## Roadmap

1. Tasks and projects (CRUD), basic Angular list UI, Docker Compose with PostgreSQL
2. Due dates and a natural-language quick-add parser ("pay rent tomorrow 5pm #home p1")
3. Recurring tasks
4. Labels, priorities, filter query language (`today | overdue & #work`)
5. Users and authentication
6. Today / Upcoming views, keyboard shortcuts, polish
7. Extract the `notification` service; add a message broker
8. Redis caching, Keycloak, observability
