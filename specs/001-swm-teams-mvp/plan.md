# Implementation Plan: SWM Teams MVP

**Branch**: `001-swm-teams-mvp` | **Date**: 2026-04-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-swm-teams-mvp/spec.md`

## Summary

Build the SWM Teams MVP around a shared `Team` root with two independently activated sub-services:
`Calendar` and `Match`. The first implementation slice should establish team onboarding, membership,
roles, and sub-service activation, then add Calendar availability integration and Match campaign/request
flows as separate domain areas.

The technical approach is a Kotlin/Spring Boot 4 REST API with domain-first modules, controller/service DTO
boundaries, implement-layer readers/writers, Exposed/Flyway persistence in `storage:db-core`, AI-assisted
duplicate analysis for Match candidate ideas, and Kotest-based tests. The existing `core:core-api` module
remains the runnable API module initially, while the domain model is split into dedicated modules as the feature
is implemented.

## Technical Context

**Language/Version**: Kotlin 2.2.21, JDK 21

**Primary Dependencies**: Spring Boot 4.0.5, Spring WebMVC, Spring Validation, Spring OAuth2 Client,
Jackson Kotlin, Exposed 1.2.0, Flyway, Spring RestDocs, server-side AI duplicate analysis adapter

**Storage**: MySQL 8.0 in production, H2 for local/test; schema changes through Flyway only; Exposed Table
definitions and explicit row mapping in `storage:db-core`

**Testing**: Kotest 6.1.11, mockk 1.14.9, springmockk, Spring RestDocs, H2-backed repository tests

**Target Platform**: Spring Boot REST API running on AWS EC2 with RDS MySQL; web frontend and Chrome extension
consume the API but are outside this backend implementation plan

**Project Type**: Multi-module Kotlin/Spring backend service

**Performance Goals**: Unified availability view p95 <= 3 seconds for initial MVP scope of about 100 teams;
duplicate mentoring schedule push must remain idempotent under repeat requests; duplicate analysis must scan
about 100 teams' released service profiles and candidate ideas without requiring user retry

**Constraints**:

- All protected APIs require authentication and team-scope authorization.
- OAuth secrets, refresh tokens, and encryption keys must remain server-side.
- External URL parsing is limited to allowed When2meet domains.
- `Calendar` and `Match` must not depend on each other's domain model or storage tables.
- API response bodies stay minimal and YAGNI-compliant.
- Boolean response fields must not be nullable; empty collections return empty arrays.
- Candidate ideas are private team data. Duplicate analysis may compare against private candidate ideas, but
  responses must not expose another team's private raw text, team identity, or source id.
- AI provider credentials and raw prompt/response payloads stay server-side and are not exposed through API
  responses or client logs.

**Scale/Scope**: Initial target is one Software Maestro cohort, approximately 300 users and 100 teams.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Plan |
|------|--------|------|
| DTO boundaries | PASS | Use controller request/response DTOs for web boundaries and service request/response DTOs when service requirements diverge. |
| Controller orchestration | PASS | Keep controllers thin; introduce Facade when onboarding or sub-service activation coordinates multiple services. |
| Domain boundaries | PASS | Own behavior by `Team`, `Calendar`, and `Match`; cross-domain references use ids. |
| Implement layer | PASS | Use `XXXReader`, `XXXWriter`, `XXXAdder`, or equivalent implement-layer components for repository reads/writes and repeated checks. |
| Persistence | PASS | Use Exposed Tables, row mapping objects, repository adapters, and Flyway migrations in `storage:db-core`; no JPA/Querydsl/SchemaUtils production usage. |
| Repository naming | PASS | Domain interfaces use `[Domain]Repository`; storage adapters use `[Domain]ExposedRepository`. |
| Testing | PASS | Controller tests use Kotest + RestDocs, service tests use Kotest + mockk, repository tests use H2/Spring or Exposed transaction setup. |
| API design | PASS | REST endpoints use `/api/{version}/...`, kebab-case resources, camelCase body fields, and plural resource names. |
| Security | PASS | Enforce authentication, team-scope authorization, server-side token handling, SSRF-safe When2meet parsing, and redacted duplicate-analysis results for private candidate ideas. |

## Project Structure

### Documentation (this feature)

```text
specs/001-swm-teams-mvp/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md
```

`plan.md` is this document. `research.md`, `data-model.md`, `quickstart.md`, `contracts/`, and `tasks.md`
are expected next Spec Kit outputs and should be generated from this plan and the feature spec.

### Source Code (repository root)

```text
core/
├── core-api/
│   └── src/main/kotlin/swm/calender/core/api/
│       ├── controller/v1/
│       ├── facade/
│       └── config/
├── core-enum/
├── core-common/
└── core-team-domain/

calendar/
└── calendar-domain/

match/
└── match-domain/

clients/
├── google-calendar/
├── when2meet/
└── client-example/

storage/
└── db-core/
    └── src/main/
        ├── kotlin/swm/calender/storage/db/core/
        │   ├── team/
        │   ├── calendar/
        │   └── match/
        └── resources/db/migration/

tests/
└── api-docs/

support/
├── logging/
└── monitoring/
```

**Structure Decision**: Implement toward the documented domain split while keeping `core:core-api` as the
runnable API module for now. Add domain modules only when their story needs them, and keep storage adapters in
`storage:db-core`. The current `clients:client-example` remains untouched unless it is replaced by
`clients:google-calendar` or `clients:when2meet` during the Calendar slice.

## Phase 0: Research

Research must resolve decisions that would otherwise leak into domain or API design.

| Topic | Decision Needed | Output |
|-------|-----------------|--------|
| Internal user eligibility | Email domain, allowlist, or external roster source | Authentication policy in `research.md` |
| Team membership permissions | OWNER/MEMBER edit matrix for team profile, services, campaigns, calendar settings | Permission matrix in `research.md` |
| Calendar OAuth scopes | Exact moment to request Calendar scopes and refresh-token storage strategy | Calendar auth decision in `research.md` |
| When2meet parsing | Allowed URL patterns, parser failure behavior, and fallback response | Parser policy in `research.md` |
| Campaign taxonomy | Initial category and platform enum values | Enum list in `research.md` |
| Feedback format | Score scale, required questions, free-text constraints | Feedback schema notes in `research.md` |
| Sub-service disable behavior | Preserve, hide, or delete Calendar/Match data when deactivated | Lifecycle decision in `research.md` |
| Candidate idea privacy and AI duplicate analysis | Corpus, redaction behavior, result shape, and trust boundary | Duplicate analysis policy in `research.md` |

## Phase 1: Design

Design outputs must be concrete enough to generate tasks without deciding implementation details during coding.

### Domain Design

- Create conceptual domain models for `Team`, `TeamMember`, `SubServiceActivation`, and invite codes.
- Model `Calendar` around `TeamCalendar`, `MentoringSchedule`, `When2meetLink`, `AvailabilitySlot`, and
  `UnifiedAvailability`.
- Model `Match` around `ServiceProfile`, `BetaCampaign`, `CandidateIdea`, `DuplicateAnalysis`, `MatchRequest`,
  `Assignment`, `Feedback`, and `Notification`.
- Keep `Calendar` and `Match` isolated; shared references are through `TeamId` or user ids.
- Keep service profile versioning explicit with one active profile per team.
- Treat candidate ideas as private team-scoped drafts and keep duplicate-analysis redaction behavior in the
  domain/service flow, not only at presentation time.
- Treat test history as assignment/feedback history only.

### API Contract Design

Initial contracts should cover:

- Team creation, invite-code join, member list, role changes, and sub-service activation.
- Calendar activation, mentoring schedule bulk push, When2meet link registration, and unified availability query.
- Match service profile publish/update, campaign create/update/open/close, campaign search/filter/sort.
- Match candidate idea create/list and duplicate-analysis execution/result retrieval.
- Match request create/accept/reject/cancel, assignment detail, feedback submission, team test history.
- Notification list/read operations if required by user-story acceptance criteria.

### Persistence Design

- Add Flyway migrations for each aggregate slice.
- Define Exposed `XXXTable` objects in `storage:db-core` packages by domain.
- Use explicit row mapping objects where table rows differ from domain models.
- Enforce NOT NULL, unique invite codes, one-active-profile-per-team, and duplicate mentoring schedule prevention
  at schema or repository level as appropriate.
- Persist candidate ideas as private team-scoped records and duplicate-analysis result summaries separately from
  raw AI prompts or provider responses.
- Use explicit joins only within aggregate boundaries or for read models needed by API queries.

### Test Design

- API contracts are verified through Kotest controller tests with RestDocs snippets.
- Service flows use mockk-based behavior tests focused on repository/implement-layer interactions.
- Domain model tests cover state transitions and business constraints.
- Repository tests use H2 with MySQL compatibility and Exposed/Spring transaction setup.
- Idempotency tests cover repeated mentoring schedule bulk push and duplicate Match requests.
- Duplicate-analysis tests cover private candidate authorization, corpus inclusion, public-source detail, and
  private-source redaction.

## Phase 2: Task Planning Notes

Tasks should be generated story-first and remain independently testable.

1. Foundation: module setup, shared IDs, auth/team-scope policy, error handling, persistence conventions.
2. US1: team onboarding, invite code join, roles, sub-service activation.
3. US2: Calendar activation, mentoring schedule push, When2meet parsing, unified availability.
4. US3: Match service profile, candidate ideas, duplicate analysis, and campaign discovery.
5. US4: Match request lifecycle, assignment creation, notifications.
6. US5: structured feedback and team test history.
7. US6: service profile pivot and team administration.

## Complexity Tracking

No constitution violations are planned.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
