# SWM Codex Harness Team Spec

## Goal

Provide a reusable Codex workflow for swm-teams monorepo changes across backend, web frontend, browser extension, shared packages, docs, and harness assets while keeping implementation work aligned with `AGENTS.md`.

Backend quality remains anchored on Exposed/Flyway persistence and Kotest/mockk tests. Frontend quality is anchored on app boundaries, explicit API contracts, package-script validation, and extension permission hygiene.

## Architecture Pattern

Primary pattern: Expert Pool + Pipeline.

The harness routes each request to the smallest relevant specialist pool, then runs a sequential context, plan, production, review, verification, and final-report pipeline.

Quality gate: Producer-Reviewer. The producer implements the change, then the AGENTS compliance reviewer and, when relevant, the full-stack contract reviewer check the diff before final verification.

The workflow stays repo-local and file-based. It does not require a separate runtime or automatic subagent execution.

## External Reference Notes

- VoltAgent `awesome-codex-subagents` was used as a shape reference for project-local `.codex/agents`, explicit delegation, and separating workspace-write implementers from read-only reviewers.
- VoltAgent `awesome-agent-skills` was used as a catalog reference for frontend design and web-app testing skill categories, but this harness keeps repo-specific skills instead of importing generic ones.

## Inputs

- User request and acceptance criteria.
- `AGENTS.md`.
- Existing project skills under `.agents/skills/`.
- Existing Codex subagent profiles under `.codex/agents/`.
- Relevant source files, tests, docs, Gradle configuration, package manifests, lockfiles, and app configuration.

## Outputs

- Completed code, docs, or harness changes.
- Optional `_workspace/` handoff files for large tasks.
- AGENTS compliance review result.
- Full-stack contract review result when backend and frontend contracts are involved.
- Verification command results.

## Roles

| Role | Responsibility | Reusable skill or profile | Writes |
| --- | --- | --- | --- |
| Harness orchestrator | Classify the request, choose project skills, preserve handoffs, and select validation commands. | `.agents/skills/swm-codex-harness/SKILL.md` | Optional `_workspace/00_input/request-summary.md` and `_workspace/final/result-summary.md` |
| Domain/API engineer | Implement API, DTO, domain, service, implement-layer, and module-boundary changes. | `.agents/skills/swm-teams-architecture/SKILL.md`, `.codex/agents/swm-domain-feature-engineer.toml` | Backend source and tests in owned modules |
| Storage engineer | Implement Exposed tables, repositories, queries, Flyway migrations, and repository tests. | `.agents/skills/swm-teams-exposed-storage/SKILL.md`, `.codex/agents/swm-exposed-storage-engineer.toml` | `apps/backend/storage/db-core/**` and migrations |
| Test engineer | Add or update Kotest, mockk, RestDocs, and repository tests. | `.agents/skills/swm-teams-testing/SKILL.md`, `.codex/agents/swm-kotest-test-engineer.toml` | `**/src/test/**` and API docs tests |
| Web frontend engineer | Implement web UI, routing, client state, API clients, shared UI, accessibility, and frontend tests. | `.agents/skills/swm-frontend-web/SKILL.md`, `.codex/agents/swm-frontend-web-engineer.toml` | `apps/web/**` and intentional `packages/**` shared files |
| Extension engineer | Implement browser extension manifest, popup, options, content scripts, background/service worker logic, permissions, and tests. | `.agents/skills/swm-browser-extension/SKILL.md`, `.codex/agents/swm-extension-engineer.toml` | `apps/extension/**` and intentional `packages/**` shared files |
| Full-stack contract reviewer | Review backend/frontend API contracts, shared types, auth assumptions, error behavior, and cross-app verification coverage. | `.agents/skills/swm-fullstack-contract/SKILL.md`, `.codex/agents/swm-fullstack-contract-reviewer.toml` | Optional `_workspace/03_review/fullstack-contract.md` |
| AGENTS compliance reviewer | Review the diff against `AGENTS.md` before final verification. | `.agents/skills/swm-agents-compliance/SKILL.md`, `.codex/agents/swm-agents-compliance-reviewer.toml` | Optional `_workspace/03_review/agents-compliance.md` |
| Verification runner | Run harness validation, relevant Gradle commands, and relevant frontend package scripts. | `scripts/run_codex_harness.py` | Terminal output summarized in final response |

## Routing Matrix

| Request surface | Primary skill | Review gate | Typical verification |
| --- | --- | --- | --- |
| Backend API, DTO, service, domain | `swm-teams-architecture` | `swm-agents-compliance` | `./gradlew :core:core-api:test`, then broader Gradle checks as needed |
| Backend persistence | `swm-teams-exposed-storage` | `swm-agents-compliance` | `./gradlew :storage:db-core:test`, Flyway-aware checks |
| Backend tests and API docs | `swm-teams-testing` | `swm-agents-compliance` | Targeted Gradle test task |
| Web frontend | `swm-frontend-web` | `swm-fullstack-contract` when API-dependent | Existing `apps/web/package.json` scripts |
| Browser extension | `swm-browser-extension` | `swm-fullstack-contract` when API-dependent | Existing `apps/extension/package.json` scripts and permission review |
| Shared frontend package | `swm-frontend-web` or `swm-browser-extension` based on consumer | `swm-fullstack-contract` when API-dependent | Owning package scripts under `packages/**` |
| Backend + frontend contract | `swm-fullstack-contract` plus owning producer skill | `swm-agents-compliance` and `swm-fullstack-contract` | Backend Gradle checks plus frontend package scripts |
| Harness or agent guidance | `harness` or `swm-codex-harness` | `swm-agents-compliance` | `python3 scripts/validate_codex_harness.py` |

## Phase Order

### Phase 1: Context Load

- Read `AGENTS.md`.
- Read relevant project skill files.
- Inspect the current diff and changed surface.
- For frontend work, inspect package manifests, lockfiles, app folders, and shared packages.
- Output: optional `_workspace/00_input/request-summary.md`.

### Phase 2: Routing And Plan

- Classify the request as backend architecture, storage, backend testing, web, extension, full-stack contract, docs, harness, or mixed.
- Select the smallest skill set that covers the work.
- Decide whether optional `_workspace/` handoffs are useful.
- For mixed backend/frontend work, define the API contract owner before implementation.
- Output: optional `_workspace/02_plan/change-plan.md`.

### Phase 3: Production

- Implement the change using existing project patterns.
- Keep persistence behind repository interfaces.
- Keep tests aligned with Kotest and mockk.
- Keep web code in `apps/web`, extension code in `apps/extension`, and shared code in `packages` only when intentionally reused.
- Keep frontend API clients aligned with backend DTOs and documented endpoint behavior.
- Output: code, docs, or harness assets.

### Phase 4: Review

- Review changed files against `.agents/skills/swm-codex-harness/references/agents-compliance-checklist.md`.
- Run full-stack contract review when backend API behavior and frontend clients or shared API types changed.
- Return `pass`, `fix`, or `redo`.
- Output: optional `_workspace/03_review/agents-compliance.md` and `_workspace/03_review/fullstack-contract.md`.

### Phase 5: Verification

- Run `python3 scripts/validate_codex_harness.py` when harness or agent guidance changed.
- Run targeted Gradle tasks while iterating on backend work.
- Run existing frontend package scripts while iterating on `apps/web`, `apps/extension`, or `packages`.
- Run broader verification for meaningful cross-module or cross-app work.
- Output: verification summary.

### Phase 6: Final Report

- Summarize changed files and behavior.
- Report passed or skipped verification commands.
- Note residual risks or follow-ups only when they matter.

## Mandatory AGENTS Gates

- No new JPA, `JpaRepository`, `@DataJpaTest`, Mockito, fetch join, or `ddl-auto` assumptions in active code or active agent guidance.
- Persistence uses Exposed DSL and Flyway.
- Tests use Kotest and mockk where mocks are needed.
- DTO, validation, API URI, response body, repository naming, domain, facade, and implement-layer rules follow `AGENTS.md`.
- Web code stays under `apps/web` unless it is intentional shared code under `packages`.
- Extension code stays under `apps/extension` unless it is intentional shared code under `packages`.
- Frontend apps do not import backend implementation code.
- Browser extension permissions are minimal, and secrets are not stored in extension source or browser storage.

## Failure Policy

- If AGENTS compliance is `fix`, correct bounded issues and rerun the relevant review or validation.
- If full-stack contract review is `fix`, align the backend DTO/API behavior and frontend client/type usage before final verification.
- If either review is `redo`, stop the current implementation direction and re-plan.
- If Gradle verification fails, inspect the failing task and fix it when the failure is related to the change.
- If frontend package verification fails, inspect the failing script and fix it when the failure is related to the change.
- If a command cannot run because of the local environment, report the command and blocker.

## Optional Worker Delegation

Use subagents only when the current Codex runtime and user request allow delegation.

Safe parallel slices:

- domain/API review and storage query review from the same stable input
- test authoring after public behavior is known
- web UI implementation and extension implementation after the API contract is stable
- full-stack contract review after backend and frontend draft changes exist
- compliance review after a draft diff exists

Forbidden overlaps:

- two workers editing the same file
- storage and domain workers independently changing repository contracts
- backend and frontend workers independently inventing different API shapes
- reviewer rewriting producer output without a clear finding

## Test Scenarios

### Normal Backend Flow

- Request: add or modify a feature across API, service, storage, and tests.
- Expected outputs: changed source files, Kotest/mockk tests, AGENTS compliance `pass`, relevant Gradle command summary.

### Normal Frontend Flow

- Request: add or modify a web or extension user flow.
- Expected outputs: changed app files, package-script verification summary, and full-stack contract review when API-dependent.

### Full-Stack Contract Flow

- Request: add or modify an API consumed by web or extension code.
- Expected outputs: backend and frontend changes agree on path, method, DTO fields, nullability, enum values, and error behavior.

### Harness Change Flow

- Request: modify skills, subagents, or harness docs.
- Expected outputs: updated harness assets and passing `python3 scripts/validate_codex_harness.py`.

### Failure Flow

- Failure point: a change introduces `JpaRepository`, Mockito, a mismatched frontend API response type, or unnecessary extension permission.
- Expected behavior: the relevant review returns `fix` or `redo`; implementation is corrected before final verification.
