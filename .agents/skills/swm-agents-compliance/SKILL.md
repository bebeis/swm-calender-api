---
name: swm-agents-compliance
description: Use when reviewing swm-teams changes for AGENTS.md compliance, including backend Exposed/Flyway rules, Kotest/mockk tests, frontend monorepo boundaries, extension safety, DTO boundaries, API rules, and repository naming.
---

# SWM AGENTS Compliance

Use this skill as a quality gate before finalizing a swm-teams change.

## Load First

- `AGENTS.md`
- `.agents/skills/swm-codex-harness/references/agents-compliance-checklist.md`
- The current diff or changed files.

## Review Steps

1. Identify the changed surfaces: API, DTO, service, implement layer, domain model, storage, backend tests, web frontend, browser extension, shared packages, docs, or harness assets.
2. Compare those surfaces against `AGENTS.md` and the checklist.
3. Flag stale JPA-era patterns if they appear in new active code or active guidance.
4. Check that persistence work uses Exposed/Flyway conventions.
5. Check that tests use Kotest and mockk where mocks are needed.
6. Check DTO, validation, API, response, and repository naming boundaries.
7. Check frontend monorepo boundaries: web under `apps/web`, extension under `apps/extension`, intentional shared code under `packages`, no backend implementation imports.
8. Check browser extension changes for minimal permissions and no secrets in source or browser storage.
9. Return `pass`, `fix`, or `redo`.

## Output Shape

Start with findings ordered by severity. For each finding, include:

- file and line when available
- violated AGENTS.md rule
- required fix

If no issue is found, say `pass` and list the verification commands that remain relevant.
