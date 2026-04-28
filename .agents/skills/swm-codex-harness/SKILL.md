---
name: swm-codex-harness
description: Use when running a reusable Codex workflow for swm-teams changes, coordinating project skills, handoff artifacts, AGENTS.md compliance review, and verification commands.
---

# SWM Codex Harness

Use this skill for non-trivial swm-teams work that benefits from a repeatable Codex workflow rather than a one-off edit.

## Load First

- `AGENTS.md` is the highest-priority repository rule source.
- `docs/harness/swm-codex/team-spec.md` defines the reusable phase order, role topology, and handoff files.
- `references/agents-compliance-checklist.md` turns `AGENTS.md` into a concrete review gate.
- `references/validation-commands.md` lists the verification commands and when to run each one.

## Required Inputs

- The user request and any explicit acceptance criteria.
- The current diff or intended change surface.
- Relevant project skills:
  - `swm-teams-architecture` for API, DTO, domain, service, implement-layer, and module-boundary work.
  - `swm-teams-exposed-storage` for Exposed, Flyway, repository, and storage query work.
  - `swm-teams-testing` for Kotest, mockk, RestDocs, and repository tests.
  - `swm-agents-compliance` for final AGENTS.md compliance review.

## Workflow

1. Summarize the request, affected modules, and likely verification commands.
2. Route the work to the smallest relevant project skill set.
3. Keep AGENTS.md constraints loaded while editing:
   - Exposed/Flyway for persistence.
   - Kotest/mockk for tests.
   - DTO, API, domain, implement-layer, and repository boundaries.
4. Preserve deterministic handoffs for large work under `_workspace/` when the workflow spans multiple phases.
5. Run an AGENTS compliance review before final verification.
6. Run `python3 scripts/validate_codex_harness.py` after changing harness assets or agent guidance.
7. Run the narrowest useful Gradle command while iterating, then broaden when the change risk warrants it.

## Handoff Artifacts

Use these files only when the task is large enough to justify durable intermediate notes:

- `_workspace/00_input/request-summary.md`
- `_workspace/01_context/context-map.md`
- `_workspace/02_plan/change-plan.md`
- `_workspace/03_review/agents-compliance.md`
- `_workspace/final/result-summary.md`

Do not create handoff files for simple edits where they would add noise.

## Delegation Rules

Project subagents under `.codex/agents/` are role profiles, not an automatic runtime requirement. Use them only when the current Codex runtime and user request allow delegation; otherwise, apply the same role guidance locally.

Good parallel slices:

- Architecture/API review independent from storage implementation.
- Storage query review independent from service test authoring.
- AGENTS compliance review after a draft diff exists.

Avoid parallel slices that edit the same file or require each other's unfinished output.

## Completion Criteria

- The diff follows `AGENTS.md`.
- Any new persistence code uses Exposed/Flyway conventions.
- Any new tests use Kotest and mockk where mocks are needed.
- Harness structure validates with `python3 scripts/validate_codex_harness.py`.
- The final response reports the verification commands that passed or could not be run.
