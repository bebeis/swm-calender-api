# AGENTS Compliance Checklist

`AGENTS.md` is the source of truth. This checklist exists to make review deterministic and should be updated when `AGENTS.md` changes.

## Persistence And Schema

- Use Exposed `Table` objects in `storage-db` code.
- Use explicit row mapping objects only where they clarify the storage boundary.
- Manage schema changes with Flyway migrations.
- Use explicit Exposed DSL joins or read models for cross-table reads.
- Keep other aggregates referenced by id, not storage-object relationships.
- Spring-managed application flows use service/implement-layer `@Transactional`; Exposed repositories do not open redundant `transaction {}` blocks.
- Do not introduce JPA, `JpaRepository`, Querydsl, `@DataJpaTest`, `FetchType.LAZY`, fetch joins, or `ddl-auto` guidance in new active rules.

## Domain And Layering

- Keep behavior aggregate-centered.
- Keep controller orchestration thin; introduce a Facade when multiple services would be coordinated directly.
- Keep service flow readable and move concrete reads/writes to implement-layer helpers such as `XXXReader`, `XXXWriter`, or `XXXAdder`.
- Implement-layer components depend on pure repository interfaces.
- Do not reference another domain's Service layer directly.
- Other domain implement-layer references are allowed when reuse is intentional.

## DTO And Validation

- Use controller request/response DTOs at the Web <-> ApiController boundary.
- Use service request/response DTOs at the ApiController <-> Service boundary unless one DTO genuinely satisfies both sides.
- Keep simple user input validation in controller DTOs with Spring Validation.
- Keep business rule validation in services or domain models.

## Testing

- Write tests with Kotest.
- Use mockk for mocks and behavior verification.
- Use RestDocs for controller API documentation tests.
- Test repositories with H2 plus Spring test context or Exposed transaction test setup.
- Keep `// given`, `// when`, and `// then` comments when they improve readability.

## API And Response Shape

- Use `/api/{version}/...` and Spring Boot 4.0 API versioning.
- Use kebab-case URI paths.
- Use camelCase parameters and body fields.
- Use plural resource names.
- Keep response bodies minimal; follow YAGNI.
- Avoid nullable Boolean fields.
- Use enums for limited string values.
- Return empty arrays for empty collections.

## Frontend And Extension Boundaries

- Keep web frontend code under `apps/web`.
- Keep browser extension code under `apps/extension`.
- Keep shared types, utilities, and UI pieces under `packages` only when they are intentionally reused.
- Do not import backend Kotlin implementation code from frontend apps or shared frontend packages.
- Keep frontend API clients aligned with backend endpoint paths, methods, DTO fields, nullability, enum values, and error behavior.
- Keep browser extension permissions minimal and specific.
- Do not store secrets or backend private configuration in extension source or browser storage.

## Naming

- Domain repository interface: `[Domain]Repository`.
- Exposed repository implementation: `[Domain]ExposedRepository`.
- Exposed table object: `XXXTable`.
- Optional storage row mapping object: `XXXEntity`.
- Domain objects are conceptual and do not need to mirror DB tables.
- Avoid direct setter usage; prefer meaningful domain methods such as `changeStatus()`.
- Method names start with verbs except technical factories such as `of` and `from`.
- Business numeric literals are constants.

## Review Output

Use one of these outcomes:

- `pass`: no AGENTS.md compliance issue found.
- `fix`: bounded issues can be corrected in the current change.
- `redo`: the implementation direction conflicts with repository rules and should be reworked before continuing.
