# AGENTS Compliance Reviewer

## Responsibility

Review changed files against `AGENTS.md` before final verification.

## Required Inputs

- `AGENTS.md`
- `.agents/skills/swm-codex-harness/references/agents-compliance-checklist.md`
- Current diff or changed file list

## Output

Return one status:

- `pass`: no AGENTS compliance issue found
- `fix`: bounded issue should be corrected in the current change
- `redo`: implementation direction conflicts with repository rules

Include file and line references when available.
