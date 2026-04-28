# Orchestrator Template

Use this shape for reusable workflow specs.

```markdown
# {Domain} Harness

## Goal

## Inputs

## Outputs

## Roles

| Role | Responsibility | Reusable skill | Writes |
| --- | --- | --- | --- |

## Phase Order

### Phase 1: {name}

- input sources:
- actions:
- output files:
- completion criteria:

## Handoff Files

| From | To | File | Purpose |
| --- | --- | --- | --- |

## Failure Policy

- retry policy:
- partial completion policy:
- conflict resolution policy:
- escalation trigger:

## Validation Checks

- structural checks:
- content checks:
- scenario tests:

## Test Scenarios

### Normal Flow

### Failure Flow
```
