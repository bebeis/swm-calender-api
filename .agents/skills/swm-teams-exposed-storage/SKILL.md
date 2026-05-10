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
- `references/exposed-practices.md` for Exposed 1.2.0 DSL, transaction, query, and schema review checks.

## Storage Shape

- The project uses Exposed 1.2.0. Prefer `org.jetbrains.exposed.v1.*` imports and do not introduce deprecated `org.jetbrains.exposed.sql.*` imports.
- Use Exposed DSL as the default repository implementation style. Do not introduce DAO `Entity` classes unless the task explicitly calls for DAO-style persistence and the tradeoff is documented.
- Use Exposed `Table` objects named `XXXTable`.
- Use row mapping objects named `XXXEntity` only when a storage-side row shape is useful.
- Prefer `internal` for storage implementation details that should not leak outside the module.
- Keep Exposed transactions at the repository/adapter boundary for standalone storage code. In Spring-managed flows, prefer service/implement-layer `@Transactional` and avoid redundant nested `transaction {}` unless there is a clear reason.
- Map DB rows to domain concepts at the persistence boundary. Do not leak Exposed types upward.
- Manage real schema changes with Flyway migrations. Use Exposed `SchemaUtils` only as test support or local schema-diff assistance.

## Query Rules

- Write complex conditions, dynamic queries, and projections with Exposed DSL.
- Use explicit joins for query needs; do not model cross-aggregate relationships as object graphs.
- Avoid raw SQL unless Exposed DSL is impractical or a measured performance issue requires it.
- Keep projection DTOs inside the repository package.
- Finish query execution and row-to-domain mapping inside the transaction boundary. Do not return lazy `Query`, `ResultRow`, DAO entities, or Exposed `EntityID` objects to service/API layers.
- For left joins, treat right-side columns as nullable and use null-safe row access before mapping.
- For list reads with related data, prefer one explicit join/projection query over per-row lookups. If DAO is ever used, review `referrersOn`/lazy access for N+1 behavior.

## Repository Rules

- Domain repository interface: `[Domain]Repository`.
- Exposed implementation class: `[Domain]ExposedRepository`.
- `storage:db-core` may implement domain interfaces, but app/API code should depend on interfaces, not storage classes.
- Translate `ExposedSQLException` and SQL state details into domain/application exceptions at the persistence boundary when callers need meaningful errors.

## Testing

- Repository tests should use H2 plus Spring test context or an Exposed transaction test setup.
- Use Kotest assertions/specs and mockk only where mocks are needed.
- Verify persistence behavior, mapping, and important query edge cases.
- Include tests for DB constraints, nullable left-join mapping, duplicate-key paths, pagination ordering, and transaction rollback when those behaviors are part of the change.
