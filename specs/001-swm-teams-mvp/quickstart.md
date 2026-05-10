# Quickstart: SWM Teams MVP

**Feature**: `001-swm-teams-mvp`
**Audience**: implementers validating the MVP backend

## Prerequisites

- JDK 21
- Gradle wrapper from this repository
- Local profile configured for H2 or MySQL-compatible test settings
- Google OAuth test credentials when validating Calendar integration
- A fixture or allowlist entry for at least two Software Maestro users

## Validation Flow

### 1. Run Baseline Checks

```bash
./gradlew ktlintCheck
./gradlew test
```

Generate and validate the public API documentation after controller contract changes:

```bash
./gradlew :core:core-api:test
./gradlew :core:core-api:asciidoctor
```

The AsciiDoc build must complete without missing `include::` failures, which confirms that
`apps/backend/core/core-api/src/docs/asciidoc/index.adoc` references generated RestDocs snippets that exist.

Recommended local fixtures:

- Two allowlisted users and at least two teams.
- One OWNER-driven team creation, invite join, sub-service activation, and team administration flow.
- One cross-team Match flow covering candidate idea privacy, duplicate-analysis redaction, beta request acceptance,
  assignment creation, feedback submission, notifications, and test history.
- Valid and invalid When2meet URLs, including rejection of any URL outside `https://when2meet.com`.
- Cross-team authorization attempts for private candidate ideas, requests, feedback, availability, and team resources.

### 2. Validate Team Onboarding

1. Authenticate as a cohort-allowlisted user.
2. Create a team through `POST /api/v1/teams`.
3. Confirm the response includes `teamId`, `name`, `inviteCode`, `calendarEnabled = false`,
   and `matchEnabled = false`.
4. Authenticate as a second allowlisted user.
5. Join the team through `POST /api/v1/teams/join`.
6. Confirm a second join attempt or another team creation attempt fails for the same user.

### 3. Validate Sub-Service Activation

1. As OWNER, enable `Calendar` through `PATCH /api/v1/teams/{teamId}/sub-services/calendar`.
2. Confirm the team activation response changes only `calendarEnabled`.
3. Enable `Match` through `PATCH /api/v1/teams/{teamId}/sub-services/match`.
4. Confirm both services can be enabled at the same time.

### 4. Validate Calendar MVP

1. Bind or authorize team Calendar access for the OWNER.
2. Bulk push a mentoring schedule list through `POST /api/v1/calendar/mentoring-schedules:bulk-push`.
3. Repeat the same request and confirm no duplicate schedule or event is created.
4. Register a When2meet link through `PUT /api/v1/calendar/when2meet-link`.
5. Query `GET /api/v1/calendar/availability` for a date range.
6. Confirm the response returns a `slots` array and completes within the 3 second MVP target under normal local
   conditions.

### 5. Validate Match MVP

1. As OWNER, create and publish a service profile through `POST /api/v1/match/service-profiles`.
2. Create a private candidate idea through `POST /api/v1/match/candidate-ideas`.
3. Confirm the same team can list it through `GET /api/v1/match/candidate-ideas`.
4. Run duplicate analysis through `POST /api/v1/match/candidate-ideas/{candidateIdeaId}/duplicate-analysis`.
5. Confirm the result includes scanned released-service and candidate-idea counts, overlap dimensions, and redacted
   source fields for private candidate matches.
6. Create a campaign through `POST /api/v1/match/campaigns`.
7. Open the campaign through `PATCH /api/v1/match/campaigns/{campaignId}/status`.
8. As another team member, search campaigns through `GET /api/v1/match/campaigns`.
9. Send a beta request through `POST /api/v1/match/campaigns/{campaignId}/requests`.
10. As target OWNER, accept the request through `PATCH /api/v1/match/requests/{requestId}/status`.
11. Confirm an assignment exists through `GET /api/v1/match/assignments/{assignmentId}`.
12. Submit feedback through `POST /api/v1/match/assignments/{assignmentId}/feedback`.
13. Confirm team test history includes the assignment and feedback summary through `GET /api/v1/match/test-history`.

### 6. Validate Security And Isolation

1. Call protected endpoints without authentication and expect 401.
2. Try to mutate another team's private resource and expect authorization failure.
3. Try to read another team's candidate idea directly and expect authorization failure.
4. Confirm candidate ideas never appear in campaign search results.
5. Try to parse a non-When2meet URL and expect validation failure.
6. Try to submit feedback from a team that is not assigned and expect validation failure.

## Done Criteria

- All story-level validation flows pass.
- Controller tests generate RestDocs snippets for public API contracts.
- Repository tests verify important uniqueness and mapping rules.
- `./gradlew ktlintCheck` and `./gradlew test` pass.

## Validation Log

| Date | Command | Result | Notes |
|------|---------|--------|-------|
| 2026-05-10 | `./gradlew :core:core-api:test :match:match-domain:test :storage:db-core:test` | PASS | Regenerated RestDocs snippets and verified notification, Match contract, service, and repository changes. |
| 2026-05-10 | `./gradlew :core:core-api:asciidoctor` | PASS | Rendered `index.adoc` successfully against generated snippets. |
| 2026-05-10 | `./gradlew ktlintCheck` | PASS | Kotlin style check passed after import ordering fix. |
| 2026-05-10 | `./gradlew test` | PASS | Full multi-module test suite passed. |

## Contract Validation Notes

- OpenAPI paths match implemented MVP controllers, including `GET /api/v1/notifications`.
- Sample `ExampleController` endpoints were removed from the runtime API surface because they are not part of the
  MVP OpenAPI contract.
- `PATCH /api/v1/match/requests/{requestId}/status` accepts only `ACCEPTED`, `REJECTED`, or `CANCELED` at the
  controller DTO boundary.
- `When2meetLinkRequest.url` is documented and validated as `https://when2meet.com` only.
- `ServiceProfileCreateRequest.screenshotUrls` and `public` are optional/defaulted request fields; response Boolean
  fields remain non-null.
