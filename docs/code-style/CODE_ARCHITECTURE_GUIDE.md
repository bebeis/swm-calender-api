# 코드 컨벤션

## 0. 자바 코드 컨벤션

- 코틀린 코드 컨벤션은 jetbrain 가이드를 기본으로 변형된 우테코 스타일을 따른다.

## 1. Layer간 DTO 변환

- Web <-> ApiController | 여기서 DTO 하나 필요하다.. (controller/request,response)
- ApiController <-> Service | 여기서 DTO 하나 필요해. (service/request,response에 위치)

그런데 두 개가 같은데 따로 만들기엔 귀찮으니까 일단 서비스 계층의 DTO로 사용가능하다면 하나만 만든다.

## 2. DTO 네이밍 및 패키지 구조

- Request : [도메인][기능][Request]
- Response : [도메인][기능][Response]
    - 범용적으로 사용되는 조회 쿼리 시  [도메인][Response]로 네이밍 가능하다.

**패키지**

- controller/request
- controller/response
- service/request
- service/response
  물론 DTO가 존재하지 않는 경우 패키지 자체를 생성하지 않아도 된다.

## 3. DTO 검증

- 사용자 요청 값에 대한 검증은 도메인 내부가 아닌 Controller 쪽의 DTO에서 처리하도록 한다. (예시: OrderRequest DTO에서 주문 수량이 0보다 큰지 검증한다.)
    - ex. 단순히 길이가 0보다 큰지 검증하는 경우, @NotEmpty 어노테이션을 활용하여 검증한다.
- 그 외의 핵심 비즈니스 로직 검증은 서비스 계층이나 도메인 모델 내부에서 처리하도록 한다. (예시: OrderService에서 주문 수량이 재고보다 많은지 검증한다.)
- DTO 검증은 Spring Validation을 활용하여 처리한다. (예시: @Valid 어노테이션을 활용하여 DTO 검증을 처리한다.)

## 4. 연관관계 매핑

- 같은 애그리거트에 있는 엔티티는 연관관계 매핑
- 다른 애그리거트에 있는 엔티티는 연관관계 매핑 X (id로 참조)
- 연관관계 매핑이 필요한 경우, 양방향 매핑보다는 단방향 매핑을 선호한다.
    - Aggregate Root에 종속적인 관계를 제외하면 양방향 매핑은 지양한다. (예시: Order - OrderItem)
- 엔티티 매핑 시 fetch 전략은 기본적으로 LAZY로 설정한다. (예시: @ManyToOne(fetch = FetchType.LAZY))
    - N+1 문제를 해결하기 위해서는 fetch join을 활용하여 조회하도록 한다. (예시: OrderRepository에서 Order과 OrderItem을 함께 조회할 때 fetch join을 활용한다.)
- NOT NULL 제약 조건, 유니크 제약 조건 등 구체적인 제약조건은 DB 스키마에서만 관리하도록 한다.
    - ddl-auto: none으로 운영한다.

### 스키마 관리

- flyway를 사용한다.

## 5. 도메인 형 패키지 구조 간 격벽

- Aggregate에 따른 동작을 지향해야 한다.
    - JPA 엔티티 매핑도 이에 따라 구성되어 있다.
- 하지만 쿼리 등을 해올 때 성능 상의 이슈로 다른 도메인까지 한 번에 조회해야 하는 경우가 있다.
- 이 경우, 도메인형 패키지 구조는 유지하되, 도메인 간 격벽을 허물 수 있다.
    - 단, 이 경우 DTO Projection을 활용하여 데이터를 조회해오도록 한다.
    - 이렇게 하면 결합도를 낮출 수 있다.
- Repository 클래스는 Aggregate 당 1개만 생성한다. Root 에서 하위 엔티티까지 조회할 수 있도록 한다. (예시: OrderRepository에서 Order과 OrderItem을 함께 조회)
    - Aggregate는 생각보다 작다. 보통 라이프사이클이 같은 객체들의 집합들끼리 묶이기 때문이다.
    - 정확히는 트랜잭션 일관성 경계에 속한 객체들의 집합이 애그리거트이다. (예시: Order와 OrderItem은 라이프사이클이 같기 때문에 같은 애그리거트에 속한다.)

## 6. Facade

- Controller에서 여러 Service를 호출해야 하는 경우, Facade를 도입하여 하나의 Service에서 여러 Service를 호출하도록 한다.
- Facade 패턴을 활용하면 Controller의 복잡도를 낮출 수 있다.

## 7. Implement Layer

- Service 계층은, 비즈니스 로직의 흐름을 나타내야 한다.
- 하지만 Service 계층에 Repository에서 조회해오는 로직이 들어가게 되면, Service 계층이 Data Access Layer의 기술적인 부분까지 알게 되는 문제가 생긴다.
- 뿐만 아니라, 읽기 / 쓰기에 관한 로직이 섞이게 되면, Service 계층이 복잡해지는 문제가 생긴다. (예시: if (xxx) throw new IllegalArgumentException("xxx"); 같은
  코드가 Service 계층에 존재하게 된다.)
- 그래서 Service layer 하위에 Implement layer를 만들어 (ex. XXXReader, XXXWriter, XXXAdder, ...)
  Repository에서 조회해오는 로직이나, 쓰는 로직, 더하는 로직 등을 구현하도록 한다.
- ImplementLayer 는 순수 Repository 인터페이스를 필드로 갖는다.
  참고: https://geminikims.medium.com/%EC%A7%80%EC%86%8D-%EC%84%B1%EC%9E%A5-%EA%B0%80%EB%8A%A5%ED%95%9C-%EC%86%8C%ED%94%84%ED%8A%B8%EC%9B%A8%EC%96%B4%EB%A5%BC-%EB%A7%8C%EB%93%A4%EC%96%B4%EA%B0%80%EB%8A%94-%EB%B0%A9%EB%B2%95-97844c5dab63
    - Repository 구현체는 storage-db 모듈을 런타임 의존관계로 갖는다.

- 다른 도메인의 Service Layer 참조는 불가능하지만, 다른 도메인의 Implement Layer 참조는 허용한다. Implement Layer 재사용성을 높이기 위해서이다.

## 8. 예외 처리

- 예외 객체는 각 도메인별 exception 패키지에 위치한다.
- 예외 메시지는 각 도메인 별로 관리한다. (예시: order.exception 패키지에 OrderException 클래스 생성)
- 예외 메시지는 ENUM으로 관리한다.

## 9. 테스트 코드

- Controller 테스트는 RestDocs를 활용하여 API 문서와 함께 작성한다.
- Service 테스트는 mockito를 활용하여 단위 테스트를 작성한다.
    - Service 테스트에서는 Repository를 mock 객체로 주입하여 테스트한다.
    - mockistic 하게 작성하여 실제 동작보단 행동 검증에 초점을 맞춘다.
- Repository 테스트는 H2 DB를 사용하여, @DataJpaTest 어노테이션을 활용하여 작성한다.
- given-when-then 패턴을 활용하여 테스트 코드를 작성한다. (가능하면)
    - 주석으로 given-when-then 구분을 명확히 한다. (예시: // given, // when, // then)

## 10. API 설계

API 설계는 최대한 RESTful 하게 구현하되, 다음과 같은 원칙을 따른다.

### 요청

- 리소스는 테이블과 1:1 매칭 대상이 아니다
- Stateless API 를 구성한다
- URI에 API와 버전을 표기한다
    - /api/{version}/리소스 템플릿을 쓴다.
    - Spring Boot 4.0에서 추가된 api versioning 기능을 사용한다.
- Path에 리소스 ID는 가능한 1개만 사용한다
    - 리소스 사이의 관계가 변하면 유연성이 떨어진다. 간단하게 유지한다.
    - 하위 리소스를 조회하는데 상위 리소스의 ID까지 필요 하다면 그건 이미**하위 리소스의 ID가 유니크 하지 않음을 의미**한다.
- URI는 kebab-case를 사용한다.
- Parameter, Body는 camelCase
- 리소스는 복수형으로 사용한다.
- 복잡한 행위에는 동사를 URI에 포함시킨다
- 일관된 용어를 사용한다.

### 응답 body

- 최소 스펙을 유지한다. (YAGNI)
    - **이미 존재하는 필드를 수정/삭제 하는 것은 너무나 어렵다**.
- body에 camelCase를 사용한다.
- Boolean 타입에는 null 이 있으면 안된다.
- 제한된 문자열 값은 열거형 (Enum) 으로 표현한다.
- 복수형 빈 값은 빈 배열로 반환한다.
- 필드명 축약 금지
- 타입에 맞는 필드명을 사용한다.

## 11. 기타

- 도메인 모듈에 있을 순수 Repository 인터페이스는 [도메인명][Repository]로, JpaRepository를 상속하는 storage-db 모듈의 인터페이스는 [도메인명][JpaRepository]로
  명명한다.
- storage-db 모듈에서 사용하는 엔티티는 XXXEntity로 명명한다. (ex. UserEntity)
- 도메인 객체는 Entity와 다르다. 도메인 객체는 테이블을 반영하는 객체가 아니다. 개념 객체이다.
    - 반드시 도메인:Entity가 1:1일 필요는 없다. 물론 대부분 1:1일 확률이 높지만, N:1이 될 수도 있다.
- setter 메서드는 지양한다. (예시: Order 객체의 상태를 변경할 때, setStatus() 메서드 대신 changeStatus() 메서드를 활용한다.)
    - kotlin에서 var로 setter를 열어는 두되, setter를 사용하지 않는다.
- 도메인 객체 생성자의 파라미터가 많을 경우 named parameter로 생성한다. 필드를 헷갈리지 않게 하기 위함!
- Lombok의 @Slf4j 어노테이션을 활용하여 로그를 남긴다. (필요한 경우)
- 불필요한 주석은 지양한다. (예시: // getter, setter 등 Lombok으로 자동 생성되는 메서드에 대한 주석은 지양한다.)
- 메서드 이름은 동사로 시작하도록 한다. (예시: createOrder(), getOrderById() 등)
    - 정적 팩토리 메서드의 경우 of, from을 써도 된다. 기술적인 (ex.ApiResponse)의 경우 of, from이 더 잘 어울리고, 도메인을 담는 경우 동사형을 쓰는 것이 좋다.
- 변수 이름은 명확하게 작성한다. (예시: orderId, userId 등)
- 상수는 대문자로 작성한다.
- 비즈니스 제약 조건(ex. 1개 이상)에서 숫자 1을 직접 사용하는 경우, 상수로 관리한다.
- 메서드 모듈화를 신경쓴다. 예를 들면, if (xxx) throw new IllegalArgumentException("xxx"); 이런 경우, validateXxx() 메서드를 만들어서 해당 메서드에서
  검증하도록 한다. (예시: validateOrderStatus() 메서드에서 주문 상태를 검증하도록 한다.)
- 도메인 모델 패턴에 따라, 도메인 로직을 최대한 도메인 모델 내부에 위치하도록 한다. (예시: Order 객체의 상태 변경 로직은 Order 객체 내부에 위치하도록 한다.)
    - Tell, Don't Ask 원칙을 지키도록 한다. (예시: Order 객체의 상태를 변경할 때, Order 객체에 changeStatus() 메서드를 호출하여 상태를 변경하도록 한다.)
- Repository에서 조회해오는 로직이 재활용가능하고 복잡한 경우, Service와 Repository 사이에 ImplementLayer(XXXReader, XXXWriter 등)를 만들어서 해당 로직을
  구현하도록 한다. (예시: OrderReader 인터페이스를 만들어서 OrderRepository에서 조회해오는 로직을 구현하도록 한다.)
- JpaRepository를 상속받는 인터페이스의 쿼리 메서드 이름이 너무 길어지는 경우, querydsl을 활용하여 쿼리를 작성하도록 한다.
- Repository에서 DTO 프로젝션을 할 때, DTO는 Repository 패키지 내부에 위치하도록 한다. (예시: OrderRepository.OrderSummaryDTO)
    - 레이어간 단방향 의존관계를 유지하기 위함이다.
