# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SWM Calendar Sync — a Spring Boot backend API for a Chrome extension that syncs Software Maestro (소마) mentoring
schedules with Google Calendar. Teams of 3 trainees share a team Google Calendar; the extension parses mentoring
schedules from the SWM site and pushes them to the calendar, and pulls team availability back.

## Tech Stack

- **Language**: Kotlin, JDK 21
- **Framework**: Spring Boot 4.0.x (WebMVC, JPA, Security OAuth2 Client, Validation, Actuator)
- **Build**: Gradle 9.4.1 (Kotlin DSL)
- **Database**: MySQL 8.0 (production), H2 (local/test)
- **External API**: Google Calendar API v3
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

- Team calendars are auto-created server-side via `calendars.insert` when a team leader creates a team
- Only `freebusy` data is queried for availability (no personal event details stored)
- CORS restricted to `swmaestro.ai` and extension origin
- Kotlin compiler flags: `-Xjsr305=strict` (strict nullability from Java annotations),
  `-Xannotation-default-target=param-property`
