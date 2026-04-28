#!/usr/bin/env python3
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def gradle_task(module: str, task: str) -> str:
    normalized = module.strip(":")
    if not normalized:
        return task
    return f":{normalized}:{task}"


def run(command: list[str]) -> int:
    print("+ " + " ".join(command), flush=True)
    completed = subprocess.run(command, cwd=ROOT, check=False)
    return completed.returncode


def build_commands(mode: str, module: str | None) -> list[list[str]]:
    commands: list[list[str]] = [
        [sys.executable, "scripts/validate_codex_harness.py"],
    ]

    if mode == "structure":
        return commands

    if module:
        if mode == "quick":
            commands.append(["./gradlew", gradle_task(module, "test")])
        else:
            commands.append(
                [
                    "./gradlew",
                    gradle_task(module, "ktlintCheck"),
                    gradle_task(module, "test"),
                ]
            )
        return commands

    if mode == "quick":
        commands.append(["./gradlew", "test"])
    else:
        commands.append(["./gradlew", "ktlintCheck", "test"])

    return commands


def main() -> int:
    parser = argparse.ArgumentParser(description="Run the swm-teams Codex harness.")
    parser.add_argument(
        "--mode",
        choices=("structure", "quick", "full"),
        default="quick",
        help="structure validates harness files only, quick adds tests, full adds ktlint and tests.",
    )
    parser.add_argument(
        "--module",
        help="Optional Gradle module path such as storage:db-core for targeted checks.",
    )
    args = parser.parse_args()

    for command in build_commands(args.mode, args.module):
        status = run(command)
        if status != 0:
            return status

    return 0


if __name__ == "__main__":
    sys.exit(main())
