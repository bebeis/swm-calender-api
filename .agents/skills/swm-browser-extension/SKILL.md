---
name: swm-browser-extension
description: Use when implementing or reviewing the swm-teams browser extension under apps/extension, including manifest, popup, options, content scripts, background workers, permissions, and extension tests.
---

# SWM Browser Extension

Use this skill for browser extension work in the swm-teams monorepo.

## When to Use

- The change touches `apps/extension/**`.
- The change adds or modifies extension manifest files, popup UI, options UI, content scripts, background or service worker logic, browser storage, permissions, or extension packaging.
- The change touches `packages/**` because extension code shares types, utilities, or UI pieces with the web app.
- Do not use this skill for web-only pages or backend-only behavior unless the extension contract also changes.

## Required Inputs

- Target browser or browser family.
- Extension surface: popup, options, content script, background/service worker, or shared package.
- Required permissions and the reason each permission is needed.
- Backend API contract or web app integration contract when the extension calls project services.
- Existing package manager, bundler, and manifest version if already present.

## Repository Boundaries

- Extension app code belongs in `apps/extension/**`.
- Shared code belongs in `packages/**` only when it is intentionally reused by web or another app.
- Prefer Manifest V3 when no existing extension manifest chooses otherwise.
- Keep extension permissions minimal and specific.
- Do not store secrets, long-lived credentials, or backend private configuration in extension source or browser storage.

## Workflow

1. Inspect `apps/extension`, package manifests, manifest files, and shared packages before editing.
2. Identify the extension execution context and browser APIs involved.
3. Define message boundaries between popup, content scripts, background/service worker, and web pages before wiring implementation.
4. Compare external API calls against backend DTOs or documented contracts. Use `swm-fullstack-contract` for cross-stack changes.
5. Implement with existing extension and frontend conventions.
6. Validate permissions, manifest shape, CSP-impacting code, package build output, and tests where the toolchain supports them.

## Outputs

- Updated extension app files under `apps/extension/**`.
- Optional shared package files under `packages/**`.
- Permission or manifest rationale in the final summary when permissions changed.
- Verification summary with package scripts that ran.

## Validation

Use the package manager already declared by `packageManager` or lockfiles. Run only scripts that exist in the target `package.json`.

Typical checks:

- `lint`
- `typecheck`
- `test`
- `build`

For manifest or permission changes, manually inspect the generated manifest or source manifest and report the changed permissions.

