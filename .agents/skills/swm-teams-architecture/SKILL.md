---
name: swm-teams-architecture
description: Use when implementing or reviewing swm-teams backend features, API boundaries, DTOs, domain models, service and implement layers, module boundaries, or Calendar/Match/Team domain behavior in this Kotlin Spring Boot project.
---

# SWM Teams Architecture

Use this skill for feature work that changes API shape, domain behavior, service orchestration, module boundaries, or cross-domain collaboration.

## Load First

- `AGENTS.md` is the source of truth for coding rules.
- `README.md` gives the product split: `Team` root, `Calendar`, and `Match`.
- Read `docs/v1/plan/PRD.md` when behavior or scope is unclear.
- Read `docs/module-structure/README.md` for intended module direction, but prefer `AGENTS.md` when persistence guidance conflicts.

## Workflow

1. Identify the owning domain: `Team`, `Calendar`, `Match`, or cross-cutting support.
2. Keep controller orchestration thin. Introduce a Facade when a controller would coordinate multiple services.
3. Keep simple request validation in controller DTOs with Spring Validation.
4. Keep business rules in services or domain models.
5. Use separate DTOs at boundaries unless one DTO genuinely satisfies both sides:
   - Web <-> ApiController: controller request/response DTO.
   - ApiController <-> Service: service request/response DTO.
6. Keep domain models conceptual. Do not mirror DB tables by default.
7. Reference other aggregates by id. Do not couple `Calendar` and `Match` through storage models.
8. Put repository reads/writes behind implement-layer components such as `XXXReader`, `XXXWriter`, or `XXXAdder` when service logic starts to bloat.

## Persistence Boundary

- Domain modules define pure repository interfaces.
- `storage:db-core` implements persistence using Exposed and Flyway.
- Do not introduce JPA, `JpaRepository`, Querydsl, or entity relationship mappings.
- Cross-aggregate joins should be explicit read models or projection queries, not domain coupling.

## Verification

Prefer the narrowest useful command first:

```bash
./gradlew test
./gradlew ktlintCheck
```

For module-local work, run the matching module task when available.
