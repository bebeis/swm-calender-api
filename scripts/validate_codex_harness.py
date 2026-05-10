#!/usr/bin/env python3
from __future__ import annotations

import sys
import tomllib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

REQUIRED_FILES = [
    "AGENTS.md",
    ".agents/skills/harness/SKILL.md",
    ".agents/skills/harness/agents/openai.yaml",
    ".agents/skills/harness/references/agent-design-patterns.md",
    ".agents/skills/harness/references/agents-md-guide.md",
    ".agents/skills/harness/references/orchestrator-template.md",
    ".agents/skills/swm-codex-harness/SKILL.md",
    ".agents/skills/swm-codex-harness/agents/openai.yaml",
    ".agents/skills/swm-codex-harness/references/agents-compliance-checklist.md",
    ".agents/skills/swm-codex-harness/references/validation-commands.md",
    ".agents/skills/swm-agents-compliance/SKILL.md",
    ".agents/skills/swm-agents-compliance/agents/openai.yaml",
    ".agents/skills/swm-teams-architecture/SKILL.md",
    ".agents/skills/swm-teams-exposed-storage/SKILL.md",
    ".agents/skills/swm-teams-testing/SKILL.md",
    ".agents/skills/swm-frontend-web/SKILL.md",
    ".agents/skills/swm-frontend-web/agents/openai.yaml",
    ".agents/skills/swm-browser-extension/SKILL.md",
    ".agents/skills/swm-browser-extension/agents/openai.yaml",
    ".agents/skills/swm-fullstack-contract/SKILL.md",
    ".agents/skills/swm-fullstack-contract/agents/openai.yaml",
    ".codex/agents/swm-agents-compliance-reviewer.toml",
    ".codex/agents/swm-frontend-web-engineer.toml",
    ".codex/agents/swm-extension-engineer.toml",
    ".codex/agents/swm-fullstack-contract-reviewer.toml",
    "docs/harness/README.md",
    "docs/harness/swm-codex/team-spec.md",
    "docs/harness/swm-codex/roles/agents-compliance-reviewer.md",
    "docs/harness/swm-codex/roles/verification-runner.md",
    "scripts/run_codex_harness.py",
    "scripts/validate_codex_harness.py",
]

AGENTS_REQUIRED_TOKENS = [
    "## Project Agent Assets",
    "## Table Relationships And Schema",
    "Exposed",
    "Flyway",
    "@Transactional",
    "transaction {}",
    "Kotest",
    "mockk",
    "[도메인명]ExposedRepository",
    "XXXTable",
    "Spring Boot 4.0",
    "apps/web",
    "apps/extension",
    "packages",
]

AGENTS_STALE_TOKENS = [
    "## Entity Relationships And Schema",
    "JpaRepository",
    "@DataJpaTest",
    "Mockito",
    "Querydsl",
    "FetchType.LAZY",
    "fetch join",
    "ddl-auto",
]

GRADLE_REQUIRED_TOKENS = [
    "kotestVersion",
    "mockkVersion",
    "io.kotest:kotest-runner-junit5",
    "io.kotest:kotest-assertions-core",
    "io.kotest:kotest-extensions-spring",
    "io.mockk:mockk",
]

TEAM_SPEC_REQUIRED_TOKENS = [
    "Pipeline",
    "Producer-Reviewer",
    "AGENTS.md",
    "_workspace/",
    "Exposed",
    "@Transactional",
    "Kotest",
    "mockk",
    "Expert Pool",
    "apps/web",
    "apps/extension",
    "swm-fullstack-contract",
]

STORAGE_SKILL_REQUIRED_TOKENS = [
    "@Transactional",
    "transaction {}",
    "Spring-managed",
]


def read_text(relative_path: str) -> str:
    return (ROOT / relative_path).read_text(encoding="utf-8")


def fail(message: str, failures: list[str]) -> None:
    failures.append(message)


def check_required_files(failures: list[str]) -> None:
    for relative_path in REQUIRED_FILES:
        if not (ROOT / relative_path).is_file():
            fail(f"Missing required file: {relative_path}", failures)


def parse_skill_frontmatter(path: Path, failures: list[str]) -> None:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()
    relative_path = path.relative_to(ROOT)
    if not lines or lines[0].strip() != "---":
        fail(f"Skill is missing YAML frontmatter: {relative_path}", failures)
        return

    closing_index = None
    for index, line in enumerate(lines[1:], start=1):
        if line.strip() == "---":
            closing_index = index
            break

    if closing_index is None:
        fail(f"Skill frontmatter is not closed: {relative_path}", failures)
        return

    fields: dict[str, str] = {}
    for line in lines[1:closing_index]:
        if not line.strip():
            continue
        if ":" not in line:
            fail(f"Invalid frontmatter line in {relative_path}: {line}", failures)
            continue
        key, value = line.split(":", 1)
        fields[key.strip()] = value.strip()

    for required_field in ("name", "description"):
        if not fields.get(required_field):
            fail(f"Skill frontmatter missing {required_field}: {relative_path}", failures)


def check_skill_frontmatter(failures: list[str]) -> None:
    for path in sorted((ROOT / ".agents/skills").glob("*/SKILL.md")):
        parse_skill_frontmatter(path, failures)


def check_agents_guidance(failures: list[str]) -> None:
    text = read_text("AGENTS.md")
    for token in AGENTS_REQUIRED_TOKENS:
        if token not in text:
            fail(f"AGENTS.md is missing required guidance token: {token}", failures)
    for token in AGENTS_STALE_TOKENS:
        if token in text:
            fail(f"AGENTS.md still contains stale JPA-era guidance token: {token}", failures)


def check_gradle_dependencies(failures: list[str]) -> None:
    combined = read_text("build.gradle.kts") + "\n" + read_text("gradle.properties")
    for token in GRADLE_REQUIRED_TOKENS:
        if token not in combined:
            fail(f"Gradle test dependency contract is missing: {token}", failures)


def check_harness_docs(failures: list[str]) -> None:
    team_spec = read_text("docs/harness/swm-codex/team-spec.md")
    for token in TEAM_SPEC_REQUIRED_TOKENS:
        if token not in team_spec:
            fail(f"Team spec is missing required token: {token}", failures)

    checklist = read_text(
        ".agents/skills/swm-codex-harness/references/agents-compliance-checklist.md"
    )
    for token in ("Exposed", "Flyway", "@Transactional", "transaction {}", "Kotest", "mockk", "JpaRepository"):
        if token not in checklist:
            fail(f"AGENTS compliance checklist is missing token: {token}", failures)

    storage_skill = read_text(".agents/skills/swm-teams-exposed-storage/SKILL.md")
    for token in STORAGE_SKILL_REQUIRED_TOKENS:
        if token not in storage_skill:
            fail(f"Storage skill is missing transaction guidance token: {token}", failures)


def check_codex_agents(failures: list[str]) -> None:
    agent_paths = sorted((ROOT / ".codex/agents").glob("*.toml"))
    if not agent_paths:
        fail("No Codex subagent profiles found under .codex/agents", failures)
        return

    for path in agent_paths:
        relative_path = path.relative_to(ROOT)
        try:
            data = tomllib.loads(path.read_text(encoding="utf-8"))
        except tomllib.TOMLDecodeError as error:
            fail(f"Invalid TOML in {relative_path}: {error}", failures)
            continue

        for key in ("name", "description", "sandbox_mode", "developer_instructions"):
            if not data.get(key):
                fail(f"Codex agent is missing {key}: {relative_path}", failures)

    compliance_text = read_text(".codex/agents/swm-agents-compliance-reviewer.toml")
    for token in ("AGENTS.md", "Exposed", "Kotest", "mockk"):
        if token not in compliance_text:
            fail(f"Compliance reviewer profile is missing token: {token}", failures)

    frontend_text = read_text(".codex/agents/swm-frontend-web-engineer.toml")
    for token in ("AGENTS.md", "apps/web", "packages", "swm-fullstack-contract"):
        if token not in frontend_text:
            fail(f"Frontend engineer profile is missing token: {token}", failures)

    extension_text = read_text(".codex/agents/swm-extension-engineer.toml")
    for token in ("AGENTS.md", "apps/extension", "permissions", "swm-fullstack-contract"):
        if token not in extension_text:
            fail(f"Extension engineer profile is missing token: {token}", failures)


def main() -> int:
    failures: list[str] = []

    check_required_files(failures)
    if not failures:
        check_skill_frontmatter(failures)
        check_agents_guidance(failures)
        check_gradle_dependencies(failures)
        check_harness_docs(failures)
        check_codex_agents(failures)

    if failures:
        for failure in failures:
            print(f"FAIL: {failure}")
        return 1

    print("OK: Codex harness structure and AGENTS compliance contracts passed validation.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
