---
name: harness
description: Design portable repo-local agent harnesses with reusable skills, team specs, deterministic handoff files, and validation gates.
---

# Harness

Use this skill to design or update a reusable repository-local workflow. In this repository, prefer `swm-codex-harness` for normal swm-teams implementation work and use this base skill when creating or reshaping harness structure itself.

## Required Inputs

- The domain or workflow goal.
- Expected final deliverables.
- Repository rules, especially `AGENTS.md`.
- Existing skills, subagents, docs, scripts, and validation commands.
- Quality gates and failure tolerance.

## Generated Artifacts

Generate only the artifacts that make the workflow reusable:

- `.agents/skills/{domain}-orchestrator/SKILL.md`
- `.agents/skills/{specialist}/SKILL.md`
- `.agents/skills/{specialist}/references/*`
- `docs/harness/{domain}/team-spec.md`
- `docs/harness/{domain}/roles/{role}.md`
- `_workspace/{phase}_{role}_{artifact}.md`
- validation scripts under `scripts/` when they remove repeated manual checks

Every generated `SKILL.md` must start with YAML frontmatter containing at least `name` and `description`.

## Workflow

1. Analyze the repository domain, task types, constraints, and existing guidance.
2. Choose the smallest architecture pattern that preserves quality.
3. Define stable roles, artifact names, handoffs, and failure policy.
4. Create or update reusable skills and progressive-disclosure references.
5. Integrate the workflow through a team spec or orchestrator skill.
6. Validate paths, frontmatter, handoff contracts, and at least one normal and failure scenario.

## Architecture Selection

Read `references/agent-design-patterns.md` before finalizing the shape.

Default to a single-agent or pipeline workflow. Add fan-out, expert routing, producer-reviewer gates, supervisor behavior, or shallow hierarchy only when the work genuinely needs it.

## AGENTS.md Guidance

Read `references/agents-md-guide.md` before editing `AGENTS.md`.

Keep `AGENTS.md` short, repo-wide, and pointer-heavy. Put task-specific workflow detail in skills, team specs, or references instead.

## Validation

For this repository, run:

```bash
python3 scripts/validate_codex_harness.py
```

Use `scripts/run_codex_harness.py` when the harness validation should be combined with Gradle checks.
