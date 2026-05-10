# Apps

사용자에게 직접 배포되는 애플리케이션을 둔다.

```text
apps/
├── backend/    # Kotlin/Spring Boot API 서버
├── web/        # 웹 프론트엔드
└── extension/  # 브라우저 확장 프론트엔드
```

백엔드는 기존 Gradle 프로젝트 경로(`:core:core-api`, `:storage:db-core` 등)를 유지하며,
실제 파일 위치만 `apps/backend` 아래로 모았다.
