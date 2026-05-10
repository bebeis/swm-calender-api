# Module Structure

## 목적

`swm-teams`는 `Calendar`, `Match` 두 sub-service를 가지지만, 모든 도메인을 한 군데에 섞지 않는다.
공유해야 하는 것은 `Team` 중심의 공통 루트와 공통 정책까지이고, 서비스별 비즈니스는 각 도메인 모듈이 가진다.

이 문서는 현재 프로젝트의 권장 모듈 구조와 의존 방향을 정리한다.

## 권장 모노레포 구조

```text
swm-teams
├── apps
│   ├── backend
│   │   ├── app
│   │   │   └── api
│   │   ├── core
│   │   │   ├── core-common
│   │   │   ├── core-team-domain
│   │   │   └── core-enum
│   │   ├── calendar
│   │   │   └── calendar-domain
│   │   ├── match
│   │   │   └── match-domain
│   │   ├── storage
│   │   │   └── db-core
│   │   ├── clients
│   │   │   ├── google-calendar
│   │   │   └── when2meet
│   │   └── support
│   │       ├── logging
│   │       └── monitoring
│   ├── web
│   └── extension
├── packages
├── docs
├── iac
└── specs
```

현재 Gradle 프로젝트 경로는 기존 호환성을 위해 `:core:core-api`, `:storage:db-core`처럼 유지한다.
물리 파일 위치만 `apps/backend` 아래로 매핑한다.

## 모듈별 책임

### `app:api`

- 실행 모듈
- Controller, Facade, API request/response DTO, Spring Boot 진입점 보유
- 여러 도메인 서비스를 조합하는 진입 계층

### `core:core-common`

- 공통 예외, 공통 응답, 공통 유틸
- 특정 도메인에 속하지 않는 공통 코드

### `core:core-team-domain`

- `Team`, `User`, `TeamMember`, `TeamRole`, `SubServiceActivation` 같은 공통 루트 도메인
- `Calendar`, `Match`가 함께 참조하는 shared kernel

### `core:core-enum`

- 여러 모듈에서 공통으로 사용하는 enum

### `calendar:calendar-domain`

- `Calendar` 도메인의 순수 Kotlin 모델
- `TeamCalendar`, `MentoringSchedule`, `Availability`, `When2meetLink`, `GoogleCalendarBinding` 등
- Repository interface, domain service, use-case 성격의 service 보유

### `match:match-domain`

- `Match` 도메인의 순수 Kotlin 모델
- `ServiceProfile`, `BetaCampaign`, `MatchRequest`, `Assignment`, `Feedback`, `RelationEdge` 등
- Repository interface, domain service, use-case 성격의 service 보유

### `storage:db-core`

- Exposed, Flyway, DataSource 공통 설정
- Exposed `Table`, row mapping 객체, Repository 구현체 보유
- 도메인 모듈의 persistence adapter 역할

### `clients:*`

- 외부 시스템 연동 모듈
- 예: Google Calendar API, When2meet 파서/HTTP 연동

### `support:*`

- 로깅, 모니터링 같은 횡단 관심사

## 핵심 원칙

### 1. 공유 모델은 최소화한다

- `Calendar`와 `Match`가 공유하는 것은 `Team` 중심의 공통 루트까지만 허용한다.
- 각 서비스의 비즈니스 개념은 공유하지 않는다.

예시:

- 공유 가능: `Team`, `User`, `TeamMember`, `TeamRole`
- `Calendar` 전용: `Availability`, `TeamCalendar`
- `Match` 전용: `ServiceProfile`, `BetaCampaign`, `Feedback`

### 2. `domain`은 순수 Kotlin만 가진다

- `domain` 모듈에는 Exposed Table이나 row mapping 객체를 두지 않는다.
- `domain` 모듈에는 persistence framework 의존성을 두지 않는다.
- `domain` 모듈에는 비즈니스 모델, 정책, Repository interface만 둔다.

### 3. Exposed/Flyway 관련 구현은 모두 `storage:db-core`에 둔다

- 모든 Exposed `Table`은 `storage:db-core`에 둔다.
- row mapping 객체와 도메인 Repository 구현체도 `storage:db-core`에 둔다.
- 운영 schema 변경은 Flyway migration으로만 관리한다.
- 구현체와 row mapping 객체는 외부에서 직접 쓰지 않도록 `internal`을 기본으로 한다.

### 4. 서비스 간 직접 참조 대신 공통 루트 ID를 사용한다

- `Calendar`가 `Match` Entity를 직접 참조하지 않는다.
- `Match`가 `Calendar` Entity를 직접 참조하지 않는다.
- 필요하면 `TeamId` 같은 식별자를 통해 연결한다.

## 의존 방향

```text
app:api
 ├── implementation -> core:core-common
 ├── implementation -> core:core-team-domain
 ├── implementation -> core:core-enum
 ├── implementation -> calendar:calendar-domain
 ├── implementation -> match:match-domain
 ├── runtimeOnly    -> storage:db-core
 ├── implementation -> clients:google-calendar
 ├── implementation -> clients:when2meet
 ├── implementation -> support:logging
 └── implementation -> support:monitoring

calendar:calendar-domain
 └── implementation -> core:core-team-domain

match:match-domain
 └── implementation -> core:core-team-domain

storage:db-core
 ├── compileOnly -> core:core-team-domain
 ├── compileOnly -> calendar:calendar-domain
 ├── compileOnly -> match:match-domain
 └── implementation -> Exposed / Flyway
```

규칙:

- `domain -> storage` 의존은 금지
- `storage -> domain` 의존만 허용
- `calendar-domain <-> match-domain` 직접 의존은 금지
- `app:api -> storage:db-core`는 `implementation`이 아니라 `runtimeOnly`로 둔다
- `storage:db-core -> domain`은 구현을 위해서만 필요하므로 `compileOnly`로 둔다
- `app:api`는 `storage` 구현 타입을 import하지 않고, `domain`의 interface만 본다

## 이 구조의 의미

그림으로 표현하면 아래와 같다.

```text
Runnable(app:api)
 ├── implementation -> Domain
 ├── implementation -> Spring
 └── runtimeOnly    -> Storage

Storage(db-core)
 ├── implementation -> Exposed
 └── compileOnly    -> Domain
```

즉, 실행 모듈은 `domain`을 통해 비즈니스를 컴파일하고, `storage`는 런타임에만 붙는다.
반대로 `storage`는 `domain`에 정의된 Repository interface와 domain model을 구현하기 위해 컴파일 시점에만 참조한다.

## `db-core` 내부 패키지 구조

`db-core` 하나에 엔티티를 모으더라도, 패키지 격벽은 강하게 유지한다.

```text
apps/backend/storage/db-core/src/main/kotlin/.../storage/db/core
├── config
│   ├── ExposedConfig
│   ├── ExposedTransactionConfig
│   └── DataSourceConfig
├── team
│   ├── TeamTable
│   ├── TeamEntity
│   └── TeamExposedRepository
├── calendar
│   ├── TeamCalendarTable
│   ├── MentoringScheduleTable
│   ├── AvailabilityTable
│   └── CalendarExposedRepository
└── match
    ├── ServiceProfileTable
    ├── BetaCampaignTable
    ├── MatchRequestTable
    ├── AssignmentTable
    ├── FeedbackTable
    └── MatchExposedRepository
```

규칙:

- `db-core` 안에서도 `team`, `calendar`, `match` 패키지를 섞지 않는다.
- 다른 도메인 데이터를 조회해야 하면 Entity 직접 참조보다 ID 기반 조회를 우선한다.
- 복합 조회가 필요하면 Exposed DSL projection이나 전용 read model을 사용한다.
- row <-> domain 변환은 별도 Mapper 클래스 대신 Repository 구현체 내부에서 처리한다.

## Repository 분리 기준

`domain`은 포트를 정의하고, `db-core`가 이를 구현한다.

예시:

```kotlin
// calendar-domain
interface TeamCalendarRepository {
    fun save(teamCalendar: TeamCalendar): TeamCalendar
    fun findByTeamId(teamId: TeamId): TeamCalendar?
}
```

```kotlin
// storage:db-core
internal class TeamCalendarExposedRepository(
) : TeamCalendarRepository {
    ...
}
```

중요한 점:

- `app:api`는 `TeamCalendarRepositoryImpl`을 보지 않고 `TeamCalendarRepository`만 주입받는다.
- `storage:db-core`는 `domain` interface를 구현하지만, 그 구현 상세는 런타임에만 조립된다.

## 왜 이렇게 나누는가

- `Calendar`와 `Match`의 비즈니스 변경이 서로를 직접 흔들지 않게 하기 위해
- 공통 루트인 `Team`만 안정적으로 공유하기 위해
- 도메인 모델을 persistence 기술 세부사항에서 분리하기 위해
- 현재는 단일 DB를 유지하면서도, 향후 서비스별 분리를 할 수 있게 하기 위해

## 현재 프로젝트에 적용할 때의 방향

- 현재 `core:core-api`는 점진적으로 `app:api` 역할로 축소한다.
- `core` 내부의 공통 코드 중 도메인과 무관한 것은 `core-common`으로 이동한다.
- 팀/사용자/권한 관련 모델은 `core-team-domain`으로 모은다.
- `Calendar`, `Match` 비즈니스는 각각 `calendar-domain`, `match-domain`으로 분리한다.
- Exposed Table, row mapping 객체, Flyway migration, Repository 구현은 `storage:db-core`에 유지한다.

## 비권장 구조

- `Calendar`와 `Match`가 서로의 도메인 모델을 직접 참조하는 구조
- `domain` 모듈 안에 Exposed Table이나 row mapping 객체를 넣는 구조
- `db-core` 안에서 도메인 패키지 경계를 무시하고 모든 엔티티를 평평하게 두는 구조
- 공통화 명목으로 `Match` 전용 개념을 `core`에 올리는 구조
