# Exposed Practices

This reference captures reusable Exposed 1.2.0 guidance for swm-teams storage work. `AGENTS.md` remains the source of truth when there is a conflict.

## Version And Imports

- The project uses `exposedVersion=1.2.0` with the Exposed BOM.
- Use `org.jetbrains.exposed.v1.*` imports.
- Use `exposed-java-time` and `org.jetbrains.exposed.v1.javatime.*` for Java time columns unless the project deliberately switches to the Kotlin datetime module.
- Do not add deprecated pre-v1 imports such as `org.jetbrains.exposed.sql.*`.

## DSL Versus DAO

- Default to DSL repositories because this project values explicit SQL shape, aggregate boundaries, and projection queries.
- Avoid DAO `IntEntity`, `UUIDEntity`, `referrersOn`, and `.with()` unless the task explicitly accepts DAO-style behavior.
- If DAO is introduced, keep entity reads and mutations inside the transaction and review lazy relation access for N+1 queries.
- Prefer explicit DSL joins plus Kotlin grouping/mapping for read models that return parent-child data.

## Table Design Checklist

- Table objects are named `XXXTable`.
- Storage row mapping objects are named `XXXEntity` only when they clarify the persistence boundary.
- Add DB-level constraints in Flyway migrations: `NOT NULL`, unique constraints, FK constraints, and indexes.
- Mirror important constraints in Exposed table definitions so DSL code reflects the schema.
- Prefer `enumerationByName` over ordinal enum storage when an enum column is needed.
- Use nullable columns only when `NULL` has a real domain meaning.
- Use explicit `ReferenceOption` values for FK delete/update behavior when the behavior matters.
- Model many-to-many relationships with an explicit join table and composite primary key.
- Prefer UUID primary keys for externally exposed identifiers when the domain needs non-guessable IDs; otherwise follow the existing domain ID convention.

## Schema And Migration Rules

- Flyway is the production schema source of truth.
- Do not enable Exposed automatic DDL generation in production runtime configuration.
- `SchemaUtils.create`, `createMissingTablesAndColumns`, and schema actualization statements are allowed only for tests or local migration assistance.
- When using `SchemaUtils` in tests, keep it isolated from application startup paths.

## Transaction Boundaries

- With `exposed-spring-boot4-starter`, Spring can manage Exposed transactions through `@Transactional`.
- Prefer service/implement-layer `@Transactional` for Spring application flows.
- Use explicit `transaction {}` for standalone storage utilities or tests that are not Spring-managed.
- Avoid returning lazy Exposed objects outside the transaction. Execute `.map`, `.toList`, `.firstOrNull`, or domain mapping before leaving the boundary.
- Be deliberate with nested transactions. Exposed nested `transaction {}` joins the outer transaction unless nested transaction support and savepoint behavior are configured.
- Prefer exception-driven rollback over manual `rollback()` in production code unless a local conditional rollback is simpler and well-contained.

## Query Patterns

- Use `where` with `and`/`or` for compound predicates.
- Use `groupBy`, aggregate expressions, and `having` for reporting or summary queries.
- Use `Case` expressions when the classification belongs in SQL and reduces post-processing.
- Use `inSubQuery`, `exists`, or `notExists` when that makes the SQL shape clearer than a join.
- Always combine pagination with deterministic `orderBy`.
- For left joins, use null-safe access such as `getOrNull` for columns from the nullable side.
- For bulk inserts, prefer `batchInsert` over per-row `insert` loops.
- Use `upsert` only when the conflict target is backed by a real unique key or primary key constraint.

## Error Handling

- Catch `ExposedSQLException` only at the boundary where SQL details can be translated.
- Use SQL state values to distinguish duplicate-key and foreign-key failures when the caller needs domain-specific handling.
- Do not leak SQL state, table names, or storage exception details into API response bodies unless intentionally mapped to a safe application error.

## Review Prompts

- Does this repository method expose `Query`, `ResultRow`, `EntityID`, DAO entity, or Exposed table objects outside storage?
- Does every list query with related data avoid N+1 behavior?
- Does every paginated query have stable ordering?
- Does every new enum column avoid ordinal persistence?
- Does every schema change have a Flyway migration and a matching Exposed table definition?
- Does Spring-managed code avoid unnecessary nested `transaction {}` blocks?
