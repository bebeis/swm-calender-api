#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
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


def backend_commands(mode: str, module: str | None) -> list[list[str]]:
    if module:
        if mode == "quick":
            return [["./gradlew", gradle_task(module, "test")]]
        return [
            [
                "./gradlew",
                gradle_task(module, "ktlintCheck"),
                gradle_task(module, "test"),
            ]
        ]

    if mode == "quick":
        return [["./gradlew", "test"]]
    return [["./gradlew", "ktlintCheck", "test"]]


def detect_package_manager(package_dir: Path, root_package: dict[str, object] | None) -> str:
    package_manager = None
    if root_package:
        package_manager = root_package.get("packageManager")
    if isinstance(package_manager, str):
        return package_manager.split("@", maxsplit=1)[0]

    for name, manager in (
        ("bun.lock", "bun"),
        ("bun.lockb", "bun"),
        ("pnpm-lock.yaml", "pnpm"),
        ("yarn.lock", "yarn"),
        ("package-lock.json", "npm"),
    ):
        if (ROOT / name).exists() or (package_dir / name).exists():
            return manager

    return "npm"


def read_package(path: Path) -> dict[str, object] | None:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return None


def frontend_package_dirs() -> list[Path]:
    candidates = [ROOT / "apps/web", ROOT / "apps/extension"]
    packages_dir = ROOT / "packages"
    if packages_dir.is_dir():
        candidates.extend(path for path in sorted(packages_dir.iterdir()) if path.is_dir())
    return [path for path in candidates if (path / "package.json").is_file()]


def package_script_command(manager: str, package_dir: Path, script: str) -> list[str]:
    relative_dir = str(package_dir.relative_to(ROOT))
    if manager == "bun":
        return ["bun", "--cwd", relative_dir, "run", script]
    if manager == "pnpm":
        return ["pnpm", "--dir", relative_dir, "run", script]
    if manager == "yarn":
        return ["yarn", "--cwd", relative_dir, "run", script]
    return ["npm", "--prefix", relative_dir, "run", script]


def frontend_commands(mode: str) -> list[list[str]]:
    root_package = read_package(ROOT / "package.json")
    scripts_by_mode = {
        "quick": ("typecheck", "test"),
        "full": ("lint", "typecheck", "test", "build"),
    }
    commands: list[list[str]] = []

    for package_dir in frontend_package_dirs():
        package_data = read_package(package_dir / "package.json")
        if not package_data:
            continue
        scripts = package_data.get("scripts", {})
        if not isinstance(scripts, dict):
            continue
        manager = detect_package_manager(package_dir, root_package)
        for script in scripts_by_mode[mode]:
            if script in scripts:
                commands.append(package_script_command(manager, package_dir, script))

    return commands


def build_commands(mode: str, module: str | None, surface: str) -> list[list[str]]:
    commands: list[list[str]] = [
        [sys.executable, "scripts/validate_codex_harness.py"],
    ]

    if mode == "structure":
        return commands

    if surface in ("backend", "all"):
        commands.extend(backend_commands(mode, module))

    if surface in ("frontend", "all"):
        commands.extend(frontend_commands(mode))

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
    parser.add_argument(
        "--surface",
        choices=("backend", "frontend", "all"),
        default="backend",
        help="Which surface to verify after structure validation.",
    )
    args = parser.parse_args()

    for command in build_commands(args.mode, args.module, args.surface):
        status = run(command)
        if status != 0:
            return status

    return 0


if __name__ == "__main__":
    sys.exit(main())
