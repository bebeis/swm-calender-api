# Validation Commands

Use the narrowest command that can prove the current change, then broaden before finishing meaningful cross-module or cross-app work.

## Harness Structure

```bash
python3 scripts/validate_codex_harness.py
```

Run this after changing `.agents/`, `.codex/agents/`, `docs/harness/`, `AGENTS.md`, Gradle test dependencies, frontend validation contracts, or harness scripts.

## Quick Backend Verification

```bash
./gradlew test
```

Run this for typical backend changes and before handing off backend changes that affect shared contracts.

## Full Backend Verification

```bash
./gradlew ktlintCheck test
```

Run this for broad backend refactors, API shape changes, or before publishing a larger backend change.

## Frontend Verification

Use the package manager already declared by `packageManager` or lockfiles. Run only scripts that exist in the target `package.json`.

Typical script names:

```bash
lint
typecheck
test
build
```

Typical targets:

```bash
apps/web/package.json
apps/extension/package.json
packages/*/package.json
```

Run frontend checks for changes under `apps/web`, `apps/extension`, or `packages`. For extension permission or manifest changes, also inspect the changed manifest or generated manifest.

## Full-Stack Verification

Run backend and frontend checks when an API contract, shared type, auth behavior, or error shape crosses the backend/frontend boundary.

## Targeted Iteration

```bash
./gradlew :core:core-api:test
./gradlew :storage:db-core:test
./gradlew :storage:db-core:ktlintCheck
```

Use module-local commands while iterating on a narrow surface. Follow with broader verification when the behavior crosses module boundaries.

## Wrapper

```bash
python3 scripts/run_codex_harness.py --mode structure
python3 scripts/run_codex_harness.py --mode quick
python3 scripts/run_codex_harness.py --mode full
python3 scripts/run_codex_harness.py --mode quick --module storage:db-core
python3 scripts/run_codex_harness.py --mode quick --surface frontend
python3 scripts/run_codex_harness.py --mode full --surface all
```

The wrapper always runs harness structure validation first, then selected backend Gradle tasks and/or frontend package scripts unless `--mode structure` is used.
