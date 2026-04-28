# Validation Commands

Use the narrowest command that can prove the current change, then broaden before finishing meaningful cross-module work.

## Harness Structure

```bash
python3 scripts/validate_codex_harness.py
```

Run this after changing `.agents/`, `.codex/agents/`, `docs/harness/`, `AGENTS.md`, Gradle test dependencies, or harness scripts.

## Quick Project Verification

```bash
./gradlew test
```

Run this for typical backend changes and before handing off changes that affect shared contracts.

## Full Local Verification

```bash
./gradlew ktlintCheck test
```

Run this for broad refactors, API shape changes, or before publishing a larger change.

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
```

The wrapper always runs harness structure validation first, then selected Gradle tasks unless `--mode structure` is used.
