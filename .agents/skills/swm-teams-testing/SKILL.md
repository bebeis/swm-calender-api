---
name: swm-teams-testing
description: Use when adding or changing tests in swm-teams, including Kotest specs, mockk service tests, RestDocs controller tests, Exposed repository integration tests, or test migration from JUnit/Mockito style.
---

# SWM Teams Testing

Use this skill for test authoring, test migration, and verification.

## Defaults

- Write new tests with Kotest.
- Use mockk for mocks and behavior verification.
- Prefer given/when/then structure. If comments help readability, use `// given`, `// when`, `// then`.
- Keep tests focused on behavior and contract, not implementation noise.

## Controller Tests

- Use RestDocs for API documentation.
- Mock service dependencies with mockk.
- Document request fields, response fields, path parameters, and query parameters that are part of the API contract.
- Keep controller validation tests separate from service business-rule tests.

## Service Tests

- Use mockk for repository and implement-layer dependencies.
- Focus on behavior: collaborator calls, business flow, and exception cases.
- Put business-rule edge cases near the service or domain model that owns the rule.

## Repository Tests

- Use H2 with Spring test context or Exposed transaction test setup.
- Do not use `@DataJpaTest`.
- Verify save/find behavior, row-to-domain mapping, unique/nullable assumptions that matter to code, and query projections.

## Verification Commands

```bash
./gradlew test
./gradlew ktlintCheck
```

Use targeted Gradle test tasks while iterating, then run the broader task before finishing meaningful changes.
