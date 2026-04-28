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
2. Create a campaign through `POST /api/v1/match/campaigns`.
3. Open the campaign through `PATCH /api/v1/match/campaigns/{campaignId}/status`.
4. As another team member, search campaigns through `GET /api/v1/match/campaigns`.
5. Send a beta request through `POST /api/v1/match/campaigns/{campaignId}/requests`.
6. As target OWNER, accept the request through `PATCH /api/v1/match/requests/{requestId}/status`.
7. Confirm an assignment exists through `GET /api/v1/match/assignments/{assignmentId}`.
8. Submit feedback through `POST /api/v1/match/assignments/{assignmentId}/feedback`.
9. Confirm team test history includes the assignment and feedback summary through `GET /api/v1/match/test-history`.

### 6. Validate Security And Isolation

1. Call protected endpoints without authentication and expect 401.
2. Try to mutate another team's private resource and expect authorization failure.
3. Try to parse a non-When2meet URL and expect validation failure.
4. Try to submit feedback from a team that is not assigned and expect validation failure.

## Done Criteria

- All story-level validation flows pass.
- Controller tests generate RestDocs snippets for public API contracts.
- Repository tests verify important uniqueness and mapping rules.
- `./gradlew ktlintCheck` and `./gradlew test` pass.
