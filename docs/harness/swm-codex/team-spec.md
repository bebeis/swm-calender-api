# SWM Codex Harness Team Spec

## Goal

Provide a reusable Codex workflow for swm-teams changes that keeps implementation work aligned with `AGENTS.md`, especially Exposed/Flyway persistence and Kotest/mockk testing rules.

## Architecture Pattern

Primary pattern: Pipeline.

Quality gate: Producer-Reviewer. The producer implements the change, then the AGENTS compliance reviewer checks the diff before final verification.

The workflow stays repo-local and file-based. It does not require a separate runtime or automatic subagent execution.

## Inputs

- User request and acceptance criteria.
- `AGENTS.md`.
- Existing project skills under `.agents/skills/`.
- Existing Codex subagent profiles under `.codex/agents/`.
- Relevant source files, tests, docs, and Gradle configuration.

## Outputs

- Completed code, docs, or harness changes.
- Optional `_workspace/` handoff files for large tasks.
- AGENTS compliance review result.
- Verification command results.

## Roles

| Role | Responsibility | Reusable skill or profile | Writes |
| --- | --- | --- | --- |
| Harness orchestrator | Classify the request, choose project skills, preserve handoffs, and select validation commands. | `.agents/skills/swm-codex-harness/SKILL.md` | Optional `_workspace/00_input/request-summary.md` and `_workspace/final/result-summary.md` |
| Domain/API engineer | Implement API, DTO, domain, service, implement-layer, and module-boundary changes. | `.agents/skills/swm-teams-architecture/SKILL.md`, `.codex/agents/swm-domain-feature-engineer.toml` | Source and test files in owned modules |
| Storage engineer | Implement Exposed tables, repositories, queries, Flyway migrations, and repository tests. | `.agents/skills/swm-teams-exposed-storage/SKILL.md`, `.codex/agents/swm-exposed-storage-engineer.toml` | `storage/db-core/**` and migrations |
| Test engineer | Add or update Kotest, mockk, RestDocs, and repository tests. | `.agents/skills/swm-teams-testing/SKILL.md`, `.codex/agents/swm-kotest-test-engineer.toml` | `**/src/test/**` and API docs tests |
| AGENTS compliance reviewer | Review the diff against `AGENTS.md` before final verification. | `.agents/skills/swm-agents-compliance/SKILL.md`, `.codex/agents/swm-agents-compliance-reviewer.toml` | Optional `_workspace/03_review/agents-compliance.md` |
| Verification runner | Run harness validation and relevant Gradle commands. | `scripts/run_codex_harness.py` | Terminal output summarized in final response |

## Phase Order

### Phase 1: Context Load

- Read `AGENTS.md`.
- Read relevant project skill files.
- Inspect the current diff and changed surface.
- Output: optional `_workspace/00_input/request-summary.md`.

### Phase 2: Routing And Plan

- Classify the request as architecture, storage, testing, docs, harness, or mixed.
- Select the smallest skill set that covers the work.
- Decide whether optional `_workspace/` handoffs are useful.
- Output: optional `_workspace/02_plan/change-plan.md`.

### Phase 3: Production

- Implement the change using existing project patterns.
- Keep persistence behind repository interfaces.
- Keep tests aligned with Kotest and mockk.
- Output: code, docs, or harness assets.

### Phase 4: AGENTS Compliance Review

- Review changed files against `.agents/skills/swm-codex-harness/references/agents-compliance-checklist.md`.
- Return `pass`, `fix`, or `redo`.
- Output: optional `_workspace/03_review/agents-compliance.md`.

### Phase 5: Verification

- Run `python3 scripts/validate_codex_harness.py` when harness or agent guidance changed.
- Run targeted Gradle tasks while iterating.
- Run broader Gradle verification for meaningful cross-module work.
- Output: verification summary.

### Phase 6: Final Report

- Summarize changed files and behavior.
- Report passed or skipped verification commands.
- Note residual risks or follow-ups only when they matter.

## Mandatory AGENTS Gates

- No new JPA, `JpaRepository`, Querydsl, `@DataJpaTest`, Mockito, fetch join, or `ddl-auto` assumptions in active code or active agent guidance.
- Persistence uses Exposed DSL and Flyway.
- Tests use Kotest and mockk where mocks are needed.
- DTO, validation, API URI, response body, repository naming, domain, facade, and implement-layer rules follow `AGENTS.md`.

## Failure Policy

- If AGENTS compliance is `fix`, correct bounded issues and rerun the relevant review or validation.
- If AGENTS compliance is `redo`, stop the current implementation direction and re-plan.
- If Gradle verification fails, inspect the failing task and fix it when the failure is related to the change.
- If a command cannot run because of the local environment, report the command and blocker.

## Optional Worker Delegation

Use subagents only when the current Codex runtime and user request allow delegation.

Safe parallel slices:

- domain/API review and storage query review from the same stable input
- test authoring after public behavior is known
- compliance review after a draft diff exists

Forbidden overlaps:

- two workers editing the same file
- storage and domain workers independently changing repository contracts
- reviewer rewriting producer output without a clear finding

## Test Scenarios

### Normal Flow

- Request: add or modify a feature across API, service, storage, and tests.
- Expected outputs: changed source files, Kotest/mockk tests, AGENTS compliance `pass`, relevant Gradle command summary.

### Harness Change Flow

- Request: modify skills, subagents, or harness docs.
- Expected outputs: updated harness assets and passing `python3 scripts/validate_codex_harness.py`.

### Failure Flow

- Failure point: a change introduces `JpaRepository` or Mockito in active code.
- Expected behavior: AGENTS compliance returns `fix` or `redo`; implementation is corrected before final verification.
