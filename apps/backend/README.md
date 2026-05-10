# Backend

Kotlin/Spring Boot 기반 API 서버와 백엔드 전용 모듈을 둔다.

```text
backend/
├── core/
├── storage/
├── clients/
├── support/
└── tests/
```

루트에서 기존 Gradle 명령을 그대로 사용할 수 있다.

```bash
./gradlew build
./gradlew :core:core-api:bootRun
./gradlew :storage:db-core:test
```
