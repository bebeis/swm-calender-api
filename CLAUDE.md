# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`swm-teams` — a Spring Boot backend API. `swm-teams` is the root product; two independent sub-services live
underneath it, and a team can enable either, or both:

1. **`Calendar`** — team schedule integration: unify Google Calendar (API v3) and When2meet (server-side
   HTML parsing) into one availability model.
2. **`Match`** — team service promotion + mutual-beta (맞베타) matching + inter-team relation graph.

`Team` is the shared root entity that both sub-services reference.

```
swm-teams (root)
├── Calendar   Google Calendar + When2meet
└── Match      service promotion + mutual beta + relation graph
```

This project evolved from `swm-calender` (a mentoring-schedule sync tool); its functionality now lives inside
the `Calendar` sub-service, and `Match` is newly added in this pivot. Backend code still lives under the
`swm.calender` Kotlin package for historical reasons; a package rename to `swm.teams.{core,calendar,match}`
is a separate refactor and is not a prerequisite for feature work.

## Tech Stack

- **Language**: Kotlin, JDK 21
- **Framework**: Spring Boot 4.0.x (WebMVC, JPA, Security OAuth2 Client, Validation, Actuator)
- **Build**: Gradle 9.4.1 (Kotlin DSL)
- **Database**: MySQL 8.0 (production), H2 (local/test)
- **External sources**: Google Calendar API v3, When2meet (server-side HTML parsing)
- **Infra**: AWS EC2 + RDS, Terraform, GitHub Actions

## Build & Run Commands

```bash
./gradlew build          # Build the project
./gradlew bootRun        # Run the application
./gradlew test           # Run all tests
./gradlew test --tests "swm.calender.SomeTest"  # Run a single test class
./gradlew clean build    # Clean and rebuild
```

## Architecture

- Package root: `swm.calender` (`src/main/kotlin/swm/calender/`)
- Entry point: `CalenderApplication.kt` — standard Spring Boot application
- Config: `src/main/resources/application.yml`
- JPA entities use `allOpen` plugin for `@Entity`, `@MappedSuperclass`, `@Embeddable`
- OAuth tokens (Client Secret, Refresh Token) are server-side only; the Chrome extension receives only session tokens
- Team-scoped access control: all API calls cross-verify `team_id` + `user_id`

## Key Design Decisions

- `Team` is the shared root; `Calendar` and `Match` are independently activatable sub-services on top of it
- (Calendar) Team calendars are auto-created via `calendars.insert` when `Calendar` is activated for a team
- (Calendar) Only `freebusy` data is queried for Google Calendar availability (no personal event details stored)
- (Calendar) When2meet integration is server-side parsing only (SSRF-safe: host allowlist); parsed slots are
  normalized into the domain and joined with Google `freebusy` results for a unified availability view
- (Match) One team holds exactly one active service profile; older profiles are preserved for pivot history
- One user belongs to exactly one team
- CORS restricted to `swmaestro.ai`, the web frontend origin, and the extension origin
- Kotlin compiler flags: `-Xjsr305=strict` (strict nullability from Java annotations),
  `-Xannotation-default-target=param-property`
