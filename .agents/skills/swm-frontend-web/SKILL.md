---
name: swm-frontend-web
description: Use when implementing or reviewing the swm-teams web frontend under apps/web, including UI flows, client state, API consumption, accessibility, and frontend tests.
---

# SWM Frontend Web

Use this skill for web frontend work in the swm-teams monorepo.

## When to Use

- The change touches `apps/web/**`.
- The change adds or modifies reusable web UI, routing, client state, API clients, or frontend tests.
- The change touches `packages/**` because web and extension code need a shared type, utility, or UI primitive.
- Do not use this skill for backend-only API, domain, storage, or Kotest work unless the frontend contract also changes.

## Required Inputs

- User-facing behavior, screen, flow, or acceptance criteria.
- Existing frontend framework and package manager, if already present.
- Backend API contract or mock contract when the UI depends on server data.
- Existing design system, component conventions, assets, and test setup.
- Root `DESIGN.md` for product UI direction, visual tokens, layout, and interaction rules.

## Repository Boundaries

- Web app code belongs in `apps/web/**`.
- Shared code belongs in `packages/**` only when it is used by more than one app or is clearly intended to be shared.
- Do not import backend Kotlin code into frontend packages.
- Do not introduce a frontend framework or package manager when the repository already has one. If none exists, make the choice explicit in the change summary and update validation guidance.

## Workflow

1. Inspect `apps/web`, root package manifests, lockfiles, and any shared packages before choosing patterns.
2. Read root `DESIGN.md` before creating or changing user-facing UI.
3. Identify the relevant screen, route, state model, API calls, loading state, empty state, error state, and responsive layout.
4. Compare any API usage against backend DTOs or documented contracts. Use `swm-fullstack-contract` for cross-stack changes.
5. Implement with existing app conventions and `DESIGN.md` guidance. Keep UI state local unless shared state is already established or the workflow requires it.
6. Add or update tests that match the existing frontend toolchain. Prefer component or route-level tests for user-visible behavior.
7. Run the narrowest useful frontend verification commands, then broaden to build-level checks for meaningful UI or shared-package work.

## Outputs

- Updated web app files under `apps/web/**`.
- Optional shared package files under `packages/**`.
- Optional `_workspace/03_review/fullstack-contract.md` when the API contract changed.
- Verification summary with the package manager and scripts that ran.

## Validation

Use the package manager already declared by `packageManager` or lockfiles. Run only scripts that exist in the target `package.json`.

Typical checks:

- `lint`
- `typecheck`
- `test`
- `build`

For broad full-stack changes, also run the relevant backend Gradle checks from `swm-codex-harness`.
