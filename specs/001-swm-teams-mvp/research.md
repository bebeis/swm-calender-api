# Research: SWM Teams MVP

**Feature**: `001-swm-teams-mvp`
**Date**: 2026-04-28
**Input**: [spec.md](./spec.md), [plan.md](./plan.md)

## Decision: Internal User Eligibility

Use Google OAuth login plus a cohort allowlist for the MVP.

**Rationale**: The product is restricted to Software Maestro internal users, but email-domain-only validation is
too weak when users may use personal Google accounts. A cohort allowlist lets the team launch with a known user
set and later replace the source with an official roster integration.

**Alternatives considered**:

- Email domain only: rejected because it can exclude valid users or admit non-cohort accounts depending on the
  domain policy.
- Open Google login: rejected because it violates the internal-only requirement.
- External roster API first: deferred because the PRD does not identify a stable source.

## Decision: Team Permission Matrix

Use OWNER as the default mutation authority and MEMBER as a scoped contributor.

| Capability | OWNER | MEMBER |
|------------|-------|--------|
| Create team | Yes | N/A |
| Join team by invite code | N/A | Yes |
| Edit team profile | Yes | No |
| Enable or disable sub-service | Yes | No |
| Manage invite code | Yes | No |
| View activated sub-service data | Yes | Yes |
| Register or replace When2meet link | Yes | No |
| Bulk push mentoring schedules | Yes | Yes |
| Create or edit service profile | Yes | No |
| Create candidate idea | Yes | No |
| View own team's candidate ideas | Yes | Yes |
| Run duplicate analysis | Yes | No |
| Create or edit campaign | Yes | No |
| Search campaigns | Yes | Yes |
| Send beta request | Yes | Yes |
| Accept or reject incoming request | Yes | No |
| Cancel own team's outgoing request | Yes | Yes |
| Submit feedback for assigned test | Yes | Yes |
| Change roles or remove members | Yes | No |

**Rationale**: Mutations that change team identity, external integrations, public campaign content, or membership
should be controlled by OWNER. Candidate ideas are pre-release strategy data, so MEMBER can view them inside the
team but OWNER controls creation and duplicate-analysis execution. MEMBER can still perform normal
collaboration workflows such as schedule push, request sending, and feedback submission.

**Alternatives considered**:

- Let MEMBER edit team profile and activated service data broadly: rejected because the exact edit surface is
  ambiguous and raises avoidable authorization risk.
- OWNER-only for all Match actions: rejected because request sending and feedback submission are natural MEMBER
  workflows.

## Decision: Calendar OAuth Scope Timing

Request Google Calendar scopes only when a team activates `Calendar` or first uses a Calendar-only operation.
Store refresh tokens server-side, encrypted, and bind the grant to the activating user and team calendar.

**Rationale**: This follows the PRD's scope-minimization requirement and avoids requesting Calendar access from
teams that only use `Match`.

**Alternatives considered**:

- Request Calendar scopes at first login: rejected because it over-requests permissions.
- Store tokens in the client: rejected because OAuth secrets and refresh tokens must stay server-side.

## Decision: When2meet Parsing Policy

Allow only `when2meet.com` HTTPS URLs, fetch server-side with a short timeout, parse into normalized
availability slots, and persist parser status with failure details and the original link.

**Rationale**: The service needs normalized availability but must defend against SSRF and When2meet HTML changes.
Persisting parser status gives teams a clear fallback when parsing fails.

**Alternatives considered**:

- Client-side parsing: rejected because it makes parsing behavior harder to control and audit.
- Accept arbitrary scheduling URLs: rejected because the MVP only commits to When2meet.

## Decision: Campaign Taxonomy

Use small enums that can be expanded through a migration.

**Categories**: `PRODUCTIVITY`, `EDUCATION`, `COMMUNITY`, `HEALTH`, `FINANCE`, `DEVELOPER_TOOL`,
`ENTERTAINMENT`, `LIFESTYLE`, `OTHER`

**Platforms**: `WEB`, `ANDROID`, `IOS`, `CHROME_EXTENSION`, `DESKTOP`, `API`, `OTHER`

**Rationale**: Search and filter need stable string values, but over-specific taxonomy would create early churn.

**Alternatives considered**:

- Free-text categories: rejected because filtering becomes inconsistent.
- Detailed marketplace taxonomy: deferred because MVP scope is one Software Maestro cohort.

## Decision: Feedback Format

Use a 1-5 integer score for required feedback criteria plus one required free-text summary.

**Required criteria**: `USABILITY`, `VALUE`, `RELIABILITY`, `RECOMMENDATION`

**Text fields**:

- `summary`: required, 10-1000 characters
- `improvementSuggestion`: optional, up to 1000 characters

**Rationale**: A short fixed rubric supports structured comparison while still leaving room for useful written
feedback.

**Alternatives considered**:

- Free-text-only feedback: rejected because it does not satisfy structured feedback.
- Large survey forms: rejected because they raise completion friction for peer beta tests.

## Decision: Sub-Service Disable Behavior

Disabling a sub-service hides it from normal workflows but preserves existing data and audit history.

**Rationale**: Calendar bindings, campaigns, requests, assignments, and feedback are historical records. Deleting
or mutating them on disable would create data loss and make reactivation risky.

**Alternatives considered**:

- Hard delete data on disable: rejected because it can destroy campaign/request/feedback history.
- Keep everything active but deny new writes only: rejected because disabled services should not appear enabled
  to users.

## Decision: Candidate Idea Privacy And AI Duplicate Analysis

Candidate ideas are private team-scoped records. Duplicate analysis runs on demand for a candidate idea, compares
it against active public service profiles/campaign descriptions and all private candidate ideas, and returns a
redacted result when a match source is another team's private candidate idea.

**Result disclosure policy**:

- Released service match: may include public service name, public team/service identifiers, and overlap summary.
- Other-team private candidate idea match: may include similarity level and overlap dimensions, but must not include raw
  candidate text, title, owning team identity, or source id.
- Own-team candidate match: may include the team's own candidate fields with `OWN_TEAM` disclosure because the
  requester already has access.
- Failed or low-confidence analysis: returns status/failure metadata without exposing private corpus content.

**Rationale**: The feature needs to compare about 100 teams' released services and in-progress ideas, but exposing
another team's candidate idea would violate the privacy requirement and discourage early idea entry. Redacted
private matches give teams useful collision signals without turning the feature into a private idea search tool.

**Alternatives considered**:

- Compare only public service profiles: rejected because it misses the user's requested candidate-idea corpus.
- Expose matched private candidate details to explain the overlap: rejected because it violates candidate privacy.
- Auto-block profile publishing when a duplicate is found: rejected because AI duplicate analysis is advisory and
  may produce false positives.

## Decision: API Response Shape

Use the existing `ApiResponse<T>` envelope with `result`, `data`, and `error`, while keeping each `data` payload
minimal.

**Rationale**: This matches the current codebase and keeps API behavior consistent.

**Alternatives considered**:

- Raw resource responses: rejected because it diverges from the current support response type.
- Large composite responses: rejected by the YAGNI response-body rule.
