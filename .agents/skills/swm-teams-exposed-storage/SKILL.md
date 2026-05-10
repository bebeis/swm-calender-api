---
name: swm-teams-exposed-storage
description: Use when creating or modifying storage-db persistence code, Exposed Table definitions, repository implementations, Exposed DSL queries, Flyway migrations, or repository integration tests in swm-teams.
---

# SWM Teams Exposed Storage

Use this skill for persistence work in `storage:db-core`.

## Load First

- `AGENTS.md` for repository, schema, aggregate, and naming rules.
- Existing files under `apps/backend/storage/db-core/src/main/kotlin` for local Exposed patterns.
- Existing files under `apps/backend/storage/db-core/src/test/kotlin` for integration test setup.

## Storage Shape

- Use Exposed `Table` objects named `XXXTable`.
- Use row mapping objects named `XXXEntity` only when a storage-side row shape is useful.
- Prefer `internal` for storage implementation details that should not leak outside the module.
- Keep Exposed transactions at the repository/adapter boundary.
- Map DB rows to domain concepts at the persistence boundary. Do not leak Exposed types upward.
- Manage real schema changes with Flyway migrations. Use Exposed `SchemaUtils` only as test support.

## Query Rules

- Write complex conditions, dynamic queries, and projections with Exposed DSL.
- Use explicit joins for query needs; do not model cross-aggregate relationships as object graphs.
- Avoid raw SQL unless Exposed DSL is impractical or a measured performance issue requires it.
- Keep projection DTOs inside the repository package.

## Repository Rules

- Domain repository interface: `[Domain]Repository`.
- Exposed implementation class: `[Domain]ExposedRepository`.
- `storage:db-core` may implement domain interfaces, but app/API code should depend on interfaces, not storage classes.

## Testing

- Repository tests should use H2 plus Spring test context or an Exposed transaction test setup.
- Use Kotest assertions/specs and mockk only where mocks are needed.
- Verify persistence behavior, mapping, and important query edge cases.
