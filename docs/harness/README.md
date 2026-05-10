# Harness Output Specs

This repository keeps reusable Codex workflow contracts under `docs/harness/`.

## Canonical Paths

- Reusable skills: `.agents/skills/`
- Codex subagent profiles: `.codex/agents/`
- Harness team specs: `docs/harness/{domain}/team-spec.md`
- Optional role briefs: `docs/harness/{domain}/roles/`
- Optional intermediate handoffs: `_workspace/`
- Harness validation scripts: `scripts/`

## Current Harness

- `.agents/skills/harness/SKILL.md` is the base Meta Harness compatible workflow designer.
- `docs/harness/swm-codex/team-spec.md` defines the project Codex workflow.
- `.agents/skills/swm-codex-harness/SKILL.md` is the top-level orchestration skill.
- `.agents/skills/swm-agents-compliance/SKILL.md` is the AGENTS.md review gate.
- `.agents/skills/swm-frontend-web/SKILL.md` covers web frontend work in `apps/web`.
- `.agents/skills/swm-browser-extension/SKILL.md` covers browser extension work in `apps/extension`.
- `.agents/skills/swm-fullstack-contract/SKILL.md` covers backend/frontend API contract work.
- `scripts/validate_codex_harness.py` validates harness structure and AGENTS-related tool choices.
- `scripts/run_codex_harness.py` runs validation plus selected backend Gradle and optional frontend package checks.

## Rules

- Keep `AGENTS.md` focused on repo-wide rules.
- Put workflow-specific detail in skills, team specs, or role briefs.
- Keep Spring/Exposed transaction guidance aligned: service or implement-layer `@Transactional` for Spring-managed flows, explicit `transaction {}` only for tests or standalone storage utilities.
- Keep model-specific recovery logic removable.
- Update the harness validator when `AGENTS.md` changes in a way that affects Exposed, Flyway, Kotest, mockk, or agent asset contracts.
