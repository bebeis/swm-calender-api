---
name: swm-fullstack-contract
description: Use when reviewing or implementing contracts between the backend API and frontend apps, including DTO shape, generated/shared types, API clients, error handling, auth, and verification across apps.
---

# SWM Fullstack Contract

Use this skill when a change crosses backend and frontend boundaries.

## When to Use

- Backend controller, DTO, or response behavior changes and a frontend app consumes it.
- `apps/web/**`, `apps/extension/**`, or `packages/**` changes depend on backend API behavior.
- Shared API types, clients, error handling, auth flows, or versioned endpoint paths change.
- A task is ambiguous between backend and frontend ownership.

## Required Inputs

- User request and acceptance criteria.
- Backend endpoint, DTO, service response, or API documentation involved.
- Frontend API client, hook, route, screen, or extension surface involved.
- Relevant verification commands for both backend and frontend surfaces.

## Contract Checklist

- Endpoint path, method, version, and resource naming match `AGENTS.md` API rules.
- Request fields, response fields, enum values, nullability, and empty-array behavior match on both sides.
- Error and validation behavior is explicit enough for frontend states.
- Auth or identity assumptions are not duplicated differently across apps.
- Frontend code does not depend on backend persistence details.
- Shared packages contain stable contracts or utilities, not app-specific leakage.

## Workflow

1. Read the backend controller and DTO shape or documented API contract.
2. Read the frontend API client, shared type, UI state handling, and extension caller if present.
3. Compare both sides using the contract checklist.
4. If implementing, change the smallest owning side first and update the dependent side immediately after.
5. If reviewing, return `pass`, `fix`, or `redo` with concrete file references.
6. Run or recommend the narrowest backend and frontend verification commands that cover the contract.

## Outputs

- Updated backend, frontend, or shared-package files when implementation is requested.
- Optional `_workspace/03_review/fullstack-contract.md` for large or risky cross-stack work.
- Contract review status: `pass`, `fix`, or `redo`.
- Verification summary covering both backend and frontend surfaces.

## Validation

- Backend: use relevant Gradle tasks from `swm-codex-harness`.
- Web: use package scripts in `apps/web/package.json` when present.
- Extension: use package scripts in `apps/extension/package.json` when present.
- Shared packages: run the owning package's typecheck, test, and build scripts when present.

