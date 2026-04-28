# SWM Teams Constitution

## Core Principles

### I. Domain Boundaries First

SWM Teams is organized around a shared `Team` root and independent `Calendar` and `Match` sub-services.
Behavior must be designed by aggregate. `Calendar` and `Match` must not directly depend on each other's domain
models or storage tables; cross-domain references use ids or explicit read models.

### II. API And DTO Boundaries

All public APIs use `/api/{version}/...`, kebab-case resource paths, camelCase request/response fields, and
plural resource names. Web-to-controller boundaries use controller request/response DTOs. Controller-to-service
boundaries use service DTOs when controller DTOs do not fully match service needs. Simple request validation
belongs in controller DTOs; business rules belong in services or domain models.

### III. Implement Layer Over Repository Detail

Service classes express business flow and must not accumulate excessive repository lookup, persistence, or
branching detail. Use implement-layer components such as `XXXReader`, `XXXWriter`, or `XXXAdder` for repository
reads, writes, and reusable checks. Implement-layer components depend on pure domain repository interfaces.

### IV. Exposed And Flyway Persistence

`storage:db-core` owns persistence adapters. Production schema changes are managed only through Flyway
migrations. Storage models use Exposed `XXXTable` objects and explicit row mapping objects where useful.
Exposed `SchemaUtils` is allowed only as test support. JPA-era repository patterns, entity relationships,
`JpaRepository`, and Querydsl are not used for new persistence work.

### V. Kotest-Based Verification

All new tests use Kotest. Controller tests use RestDocs and document public API fields and parameters. Service
tests use mockk and focus on behavior and collaborator interactions. Repository tests use H2 with Spring test
context or Exposed transaction test setup. Prefer given/when/then structure.

## Engineering Constraints

- Kotlin follows JetBrains conventions with the project's Woowacourse-style variations.
- Domain repositories are named `[Domain]Repository`.
- Exposed repository adapters are named `[Domain]ExposedRepository`.
- Exposed tables are named `XXXTable`.
- Exceptions live in each domain's `exception` package, and exception messages are managed by enum.
- Response bodies stay minimal. Boolean response fields are non-null. Empty collection values return empty arrays.
- User/team data access must enforce authentication and team-scope authorization.
- External URL parsing, including When2meet parsing, must be SSRF-safe and allowlist-based.
- Raw SQL is allowed only when Exposed DSL cannot express the query or measured performance requires it.

## Development Workflow

- Spec Kit artifacts live under `specs/{feature}/`.
- The current active feature plan is `specs/001-swm-teams-mvp/plan.md`.
- Project-specific Codex skills live under `.agents/skills/`.
- Harness and agent documentation live under `docs/harness/`.
- When harness or agent instructions change, run `python3 scripts/validate_codex_harness.py`.
- For feature implementation, run the narrowest useful Gradle verification first, then broader checks before
  finishing meaningful changes.

## Governance

This constitution is the Spec Kit governance document for the repository. `AGENTS.md` remains the detailed
runtime instruction source for Codex in this repository; if this constitution and `AGENTS.md` conflict,
`AGENTS.md` is more specific and must be followed while this constitution is amended. Any new feature plan must
include a Constitution Check covering DTO boundaries, domain boundaries, implement layer, persistence, tests,
API design, and security.

**Version**: 1.0.0 | **Ratified**: 2026-04-28 | **Last Amended**: 2026-04-28
