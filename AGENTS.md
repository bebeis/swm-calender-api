# AGENTS.md

이 파일은 Codex가 이 저장소에서 코드를 작성하거나 수정할 때 따라야 하는 규칙이다.
원문 기준은 `docs/code-style/CODE_ARCHITECTURE_GUIDE.md`이며, 아래 규칙을 우선 적용한다.

## Kotlin Style

- Kotlin 코드는 JetBrains Kotlin coding conventions를 기본으로 하되, 프로젝트의 우테코 스타일 변형을 따른다.
- 기존 코드 스타일, 패키지 구조, 네이밍, 테스트 작성 방식과 충돌하지 않게 작성한다.

## Project Agent Assets

- 프로젝트 전용 Codex skills는 `.agents/skills/`에 둔다.
  - `harness`: Meta Harness 호환 기본 하네스를 설계하거나 수정할 때 사용한다.
  - `swm-codex-harness`: 큰 작업의 단계, handoff, 검증 흐름을 조율할 때 사용한다.
  - `swm-agents-compliance`: 변경사항이 이 파일의 규칙을 지키는지 검토할 때 사용한다.
  - `swm-teams-architecture`: API, DTO, 도메인, 모듈 경계 작업에 사용한다.
  - `swm-teams-exposed-storage`: Exposed/Flyway 기반 storage 작업에 사용한다.
  - `swm-teams-testing`: Kotest/mockk/RestDocs/Repository 테스트 작업에 사용한다.
  - `swm-frontend-web`: `apps/web` 웹 프론트엔드 작업에 사용한다.
  - `swm-browser-extension`: `apps/extension` 브라우저 확장 작업에 사용한다.
  - `swm-fullstack-contract`: 백엔드 API와 프론트엔드 클라이언트 계약을 함께 변경하거나 검토할 때 사용한다.
- 프로젝트 전용 Codex subagent는 `.codex/agents/`에 둔다.
- subagent는 자동 실행을 전제로 하지 않는다. 큰 작업에서 명시적으로 역할을 나눌 때 사용한다.
- Codex harness 문서는 `docs/harness/`에 둔다.
- harness 또는 agent 지침을 수정하면 `python3 scripts/validate_codex_harness.py`를 실행한다.

## Monorepo Boundaries

- 백엔드 Kotlin/Spring Boot 코드는 `apps/backend` 아래에 둔다.
- 웹 프론트엔드 코드는 `apps/web` 아래에 둔다.
- 브라우저 확장 코드는 `apps/extension` 아래에 둔다.
- 여러 앱이 공유하는 타입, 유틸, UI 조각은 필요할 때만 `packages` 아래에 둔다.
- 프론트엔드는 백엔드 구현 코드를 직접 import하지 않고 API contract나 shared package를 통해 연동한다.
- 브라우저 확장은 필요한 최소 권한만 선언하고 secret이나 백엔드 private 설정을 소스 또는 브라우저 저장소에 두지 않는다.

## DTO Boundaries

- Web <-> ApiController 경계에는 controller request/response DTO가 필요하다.
- ApiController <-> Service 경계에는 service request/response DTO가 필요하다.
- 두 DTO가 실질적으로 동일하고 service DTO로 controller 요구사항을 충족할 수 있으면 하나만 사용해도 된다.
- DTO 네이밍은 다음 규칙을 따른다.
  - Request: `[도메인][기능]Request`
  - Response: `[도메인][기능]Response`
  - 범용 조회 응답은 `[도메인]Response`로 네이밍할 수 있다.
- DTO 패키지는 필요할 때만 만든다.
  - `controller/request`
  - `controller/response`
  - `service/request`
  - `service/response`

## DTO Validation

- 사용자 요청 값에 대한 단순 검증은 Controller 쪽 DTO에서 처리한다.
  - 예: 길이, 비어 있음, null 여부, 양수 여부 등은 Spring Validation을 사용한다.
  - 예: `@Valid`, `@NotEmpty` 등.
- 핵심 비즈니스 규칙 검증은 Service 계층이나 Domain 모델 내부에서 처리한다.
  - 예: 주문 수량이 재고보다 많은지 확인하는 로직.
- DTO 검증과 도메인 비즈니스 검증을 섞지 않는다.

## Table Relationships And Schema

- `storage-db` 모듈의 DB 모델은 Exposed `Table`과 명시적인 row mapping 객체로 표현한다.
- 같은 Aggregate 안의 Table은 필요한 경우 FK 제약과 명시적인 join으로 함께 조회할 수 있다.
- 다른 Aggregate의 Table은 연관관계처럼 직접 결합하지 않고 id 값으로 참조한다.
- Aggregate Root에 종속적인 관계를 제외하면 Table 간 join은 필요한 조회에서만 명시적으로 사용한다.
- N+1 문제가 생기지 않도록 필요한 데이터는 Exposed DSL의 명시적인 join이나 별도 read model로 조회한다.
- NOT NULL, unique 등 구체적인 제약조건은 DB schema에서 관리한다.
- 운영 schema 변경은 Flyway migration으로만 관리한다.
- Exposed `SchemaUtils`는 테스트 보조 용도 외에는 사용하지 않는다.

## Domain Package Boundaries

- 동작은 Aggregate 기준으로 설계한다.
- 도메인형 패키지 구조는 유지한다.
- 성능 문제로 여러 도메인을 함께 조회해야 하는 경우 DTO Projection을 사용한다.
- 다른 도메인 데이터를 직접 끌어오더라도 결합도를 낮추는 방향으로 설계한다.
- Repository 클래스는 Aggregate당 하나만 만든다.
  - Aggregate Root에서 하위 Entity까지 조회할 수 있게 한다.
  - Aggregate는 트랜잭션 일관성 경계에 속한 객체들의 집합이다.

## Facade

- Controller에서 여러 Service를 호출해야 하면 Facade를 도입한다.
- Controller는 복잡한 orchestration을 직접 갖지 않게 한다.

## Implement Layer

- Service 계층은 비즈니스 로직의 흐름을 표현해야 한다.
- Service 안에 Repository 조회 로직, 쓰기 로직, 복잡한 검증 분기가 과도하게 들어가지 않게 한다.
- Service 하위에 Implement Layer를 둔다.
  - 예: `XXXReader`, `XXXWriter`, `XXXAdder`
- Implement Layer는 Repository 조회, 저장, 추가 등 구체적인 작업을 담당한다.
- Implement Layer는 순수 Repository interface를 필드로 가진다.
- Repository 구현체는 `storage-db` 모듈을 runtime dependency로 둔다.
- Spring-managed application flow의 트랜잭션 경계는 Service 또는 Implement Layer의 `@Transactional`로 관리한다.
- `storage-db`의 Exposed Repository adapter는 Spring-managed flow에서 중첩 `transaction {}`을 열지 않는다.
  - 테스트 보조, standalone storage utility처럼 Spring-managed flow 밖에서만 명시적 Exposed `transaction {}`을 사용할 수 있다.
- 다른 도메인의 Service Layer 참조는 금지한다.
- 다른 도메인의 Implement Layer 참조는 재사용성을 위해 허용한다.

## Exceptions

- 예외 객체는 각 도메인별 `exception` 패키지에 둔다.
- 예외 메시지는 도메인별로 관리한다.
- 예외 메시지는 enum으로 관리한다.

## Tests

- 모든 테스트는 Kotest를 사용해 작성한다.
- Controller 테스트는 Kotest 기반으로 RestDocs를 활용해 API 문서와 함께 작성한다.
- Service 테스트는 Kotest와 mockk를 활용한 단위 테스트로 작성한다.
  - Repository는 mock 객체로 주입한다.
  - 실제 동작보다 행동 검증에 초점을 맞춘 mockistic 테스트를 작성한다.
- Repository 테스트는 H2 DB와 Spring test context 또는 Exposed transaction test 설정을 사용한다.
- 가능하면 given-when-then 패턴을 사용한다.
  - 주석은 `// given`, `// when`, `// then`으로 구분한다.

## API Design

- API는 가능한 RESTful하게 설계한다.
- Resource는 DB table과 1:1로 매칭되는 대상이 아니다.
- API는 stateless하게 구성한다.
- URI에는 API와 version을 표기한다.
  - `/api/{version}/...` 템플릿을 사용한다.
  - Spring Boot 4.0의 API versioning 기능을 사용한다.
- Path의 resource id는 가능한 하나만 사용한다.
  - 하위 resource 조회에 상위 resource id까지 필요하다면 하위 resource id가 unique하지 않은 설계인지 점검한다.
- URI는 kebab-case를 사용한다.
- Parameter와 request/response body 필드는 camelCase를 사용한다.
- Resource 이름은 복수형을 사용한다.
- 복잡한 행위에는 동사를 URI에 포함시킬 수 있다.
- API 용어는 일관되게 사용한다.

## API Response Body

- Response body는 최소 스펙을 유지한다. YAGNI를 따른다.
- 이미 노출된 필드의 수정/삭제는 어렵다는 점을 고려해 필드를 신중히 추가한다.
- Body field는 camelCase를 사용한다.
- Boolean 타입에는 null을 허용하지 않는다.
- 제한된 문자열 값은 enum으로 표현한다.
- 복수형 빈 값은 빈 배열로 반환한다.
- 필드명은 축약하지 않는다.
- 타입에 맞는 명확한 필드명을 사용한다.

## Naming And Domain Model Rules

- Domain module의 순수 Repository interface는 `[도메인명]Repository`로 명명한다.
- `storage-db` module에서 Exposed로 Domain Repository를 구현하는 class는 `[도메인명]ExposedRepository`로 명명한다.
- `storage-db` module에서 사용하는 Exposed Table object는 `XXXTable`로 명명한다.
- `storage-db` module에서 사용하는 row mapping 객체는 필요하면 `XXXEntity`로 명명한다.
- Domain object는 DB table을 그대로 반영하는 객체가 아니라 개념 객체이다.
  - Domain object와 Entity가 반드시 1:1일 필요는 없다.
- Setter method는 지양한다.
  - 상태 변경은 `setStatus()` 대신 `changeStatus()` 같은 의미 있는 method로 표현한다.
  - Kotlin에서 `var`로 setter가 열려 있어도 직접 setter 사용은 피한다.
- Domain object 생성자 parameter가 많으면 named parameter로 생성해 필드 혼동을 줄인다.
- 필요한 경우 Lombok `@Slf4j`를 사용해 logging한다.
- 불필요한 주석은 작성하지 않는다.
- Method 이름은 동사로 시작한다.
  - 정적 factory method는 `of`, `from`을 사용할 수 있다.
  - 기술적 wrapper는 `of`, `from`이 어울리고, domain 의미를 담는 경우 동사형을 선호한다.
- 변수 이름은 `orderId`, `userId`처럼 명확하게 작성한다.
- 상수는 대문자로 작성한다.
- 비즈니스 제약 조건의 숫자 literal은 상수로 관리한다.
  - 예: "1개 이상" 조건의 `1`.
- 복잡하거나 재사용되는 검증은 `validateXxx()` method로 분리한다.
- Domain model pattern을 따르고, domain logic은 가능한 domain model 내부에 둔다.
- Tell, Don't Ask 원칙을 따른다.
  - 객체 상태 변경은 외부에서 상태를 꺼내 판단하기보다 객체의 method를 호출해 수행한다.

## Repository And Query Rules

- Repository 조회 로직이 복잡하거나 재사용 가능하면 Service와 Repository 사이에 Implement Layer를 둔다.
  - 예: `OrderReader`, `OrderWriter`
- 복잡한 조건, 동적 쿼리, projection 조회는 Exposed DSL로 명시적으로 작성한다.
- Raw SQL은 Exposed DSL로 표현하기 어렵거나 성능상 필요할 때만 제한적으로 사용한다.
- Repository에서 DTO projection을 사용할 때 projection DTO는 Repository package 내부에 둔다.
  - 레이어 간 단방향 의존관계를 유지하기 위함이다.

<!-- SPECKIT START -->
For Spec Kit governance, read `.specify/memory/constitution.md`.
For the active SWM Teams MVP feature plan, read `specs/001-swm-teams-mvp/plan.md`.
<!-- SPECKIT END -->
