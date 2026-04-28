# Verification Runner

## Responsibility

Run the validation commands that match the changed surface and summarize the result.

## Command Selection

- Harness or agent assets changed: `python3 scripts/validate_codex_harness.py`
- Typical backend change: `./gradlew test`
- Broad or publishable change: `./gradlew ktlintCheck test`
- Narrow module iteration: module-specific Gradle test or ktlint task

## Output

Report:

- commands run
- pass or fail result
- relevant failure summary
- commands skipped because of environment blockers
