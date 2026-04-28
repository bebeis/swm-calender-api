# Agent Design Patterns

Choose the smallest pattern that makes the workflow understandable and reusable.

## Single-Agent Workflow

Use when one skill can complete the task without durable role boundaries.

Artifacts: one `SKILL.md` or one team-spec section.

## Pipeline

Use when each phase depends on the previous phase.

Artifacts: ordered phase outputs such as `_workspace/01_*`, `_workspace/02_*`, plus a team spec or orchestrator skill.

## Fan-Out/Fan-In

Use when independent branches can work from the same input and later be synthesized.

Artifacts: one branch handoff per specialist and one synthesis artifact.

## Expert Pool

Use when only a subset of specialists should run for a request.

Artifacts: routing rules, specialist skills, and a fallback path for ambiguous requests.

## Producer-Reviewer

Use when generated output needs an explicit quality gate.

Artifacts: producer skill, reviewer skill or role brief, review notes, and a bounded revision policy.

## Supervisor

Use when the backlog changes during execution and work needs reassignment.

Artifacts: task inventory, reassignment policy, status format, and integration report.

## Hierarchical Delegation

Use when the problem naturally separates into a shallow tree of sub-goals.

Artifacts: top-level orchestrator, lower-level role briefs, and parent-child handoff files.

## Rippable Rule

Keep model-specific retries, temporary recovery rules, and shortcuts in removable reference sections. The core workflow should remain useful if those notes are deleted later.
