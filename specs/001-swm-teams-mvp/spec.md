# Feature Specification: SWM Teams MVP

**Feature Branch**: `001-swm-teams-mvp`
**Created**: 2026-04-28
**Status**: Draft
**Input**: PRD from `docs/v1/plan/PRD.md`

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Team Onboarding And Sub-Service Activation (Priority: P1)

소마 내부 사용자는 팀을 만들거나 초대 코드로 팀에 합류하고, 팀 단위로 `Calendar`와
`Match` sub-service를 각각 활성화할 수 있다.

**Why this priority**: 모든 `Calendar`와 `Match` 기능은 팀과 팀 멤버십을 전제로 한다.

**Independent Test**: 신규 OWNER가 팀을 생성하고 초대 코드를 발급한 뒤, 다른 사용자가
해당 코드로 합류하고 sub-service 활성화 상태가 팀에 저장되는지 검증한다.

**Acceptance Scenarios**:

1. **Given** 팀에 속하지 않은 인증 사용자가 있을 때, **When** 필수 팀 정보를 입력해 팀을
   생성하면, **Then** 사용자는 OWNER가 되고 팀 프로필과 초대 코드가 생성된다.
2. **Given** 팀에 속하지 않은 인증 사용자가 유효한 초대 코드를 입력할 때, **When** 가입을
   요청하면, **Then** 사용자는 해당 팀 MEMBER가 되고 한 사용자 1팀 제약이 유지된다.
3. **Given** 팀 OWNER가 있을 때, **When** `Calendar` 또는 `Match`를 활성화하거나 비활성화하면,
   **Then** 각 sub-service 상태는 독립적으로 저장되고 다른 sub-service 상태는 바뀌지 않는다.

---

### User Story 2 - Unified Team Availability (Priority: P1)

팀 OWNER는 팀 Google Calendar와 When2meet 링크를 연결하고, 팀원은 소마 멘토링 일정과
When2meet 가용성을 합친 통합 가용성 뷰를 확인할 수 있다.

**Why this priority**: 기존 `swm-calender`의 핵심 가치를 유지하면서 팀 일정 누락 문제를
가장 직접적으로 해결한다.

**Independent Test**: `Calendar`만 활성화한 팀에서 멘토링 일정 bulk push, 중복 방지,
When2meet 링크 등록, 통합 가용성 조회를 순서대로 실행해 검증한다.

**Acceptance Scenarios**:

1. **Given** `Calendar`를 활성화한 팀이 있을 때, **When** OWNER가 멘토링 일정 목록을
   bulk push하면, **Then** 팀 캘린더에 일정이 등록되고 같은 일정은 중복 등록되지 않는다.
2. **Given** 팀 프로필에 When2meet 링크가 등록되어 있을 때, **When** 서버 파싱이 성공하면,
   **Then** 가용성 슬롯이 팀 스코프 데이터로 정규화되어 저장된다.
3. **Given** Google Calendar 일정과 When2meet 가용성 데이터가 있을 때, **When** 팀원이
   통합 가용성 뷰를 조회하면, **Then** 두 소스가 하나의 시간대 기준으로 병합되어 표시된다.

---

### User Story 3 - Service Profile And Campaign Discovery (Priority: P1)

팀 OWNER는 `Match`에서 팀 서비스 프로필과 베타 캠페인을 공개하고, 다른 팀은 탐색 화면에서
캠페인을 검색, 필터, 정렬해 찾을 수 있다.

**Why this priority**: 흩어진 서비스 홍보를 구조화하고 맞베타 요청의 진입점을 만든다.

**Independent Test**: `Match`만 활성화한 팀이 서비스 프로필과 공개 캠페인을 만들고, 다른 팀이
탐색 조건으로 해당 캠페인을 찾을 수 있는지 검증한다.

**Acceptance Scenarios**:

1. **Given** `Match`를 활성화한 팀 OWNER가 있을 때, **When** 필수 서비스 프로필 정보를
   입력해 공개하면, **Then** 해당 프로필은 탐색 화면에 노출된다.
2. **Given** active 서비스 프로필이 있는 팀이 있을 때, **When** OWNER가 모집 팀 수, 마감일,
   맞베타 가능 여부, 요구 사항을 입력해 캠페인을 열면, **Then** 공개 캠페인은 탐색 대상이 된다.
3. **Given** 여러 공개 캠페인이 있을 때, **When** MEMBER가 카테고리, 플랫폼, 맞베타 가능 여부로
   필터링하거나 정렬하면, **Then** 조건에 맞는 캠페인 목록만 반환된다.

---

### User Story 4 - Beta Request And Assignment Lifecycle (Priority: P2)

팀 MEMBER는 다른 팀 캠페인에 단방향 베타 요청 또는 맞베타 요청을 보내고, 대상 팀 OWNER는
요청을 수락, 거절, 취소 상태로 관리할 수 있다. 수락된 요청은 테스트 할당으로 이어진다.

**Why this priority**: `Match`의 핵심 차별점인 기록 가능한 맞베타 교환을 만든다.

**Independent Test**: 두 팀이 공개 캠페인을 기준으로 요청을 주고받고, 대상 팀이 수락했을 때
테스트 할당과 상태 히스토리, 알림이 생성되는지 검증한다.

**Acceptance Scenarios**:

1. **Given** 공개 캠페인이 있을 때, **When** 다른 팀 MEMBER가 단방향 요청을 보내면,
   **Then** 요청은 pending 상태로 생성되고 대상 팀에 알림이 생성된다.
2. **Given** 공개 캠페인이 있고 요청 팀에도 active 캠페인이 있을 때, **When** 요청 팀 MEMBER가
   맞베타 요청을 보내면, **Then** 요청은 상호 테스트 의도를 포함해 생성된다.
3. **Given** pending 요청이 있을 때, **When** 대상 팀 OWNER가 요청을 수락하면, **Then**
   테스트 할당이 생성되고 요청 히스토리와 알림이 기록된다.
4. **Given** pending 요청이 있을 때, **When** 대상 팀 OWNER가 요청을 거절하거나 요청 팀이
   취소하면, **Then** 테스트 할당은 생성되지 않고 최종 상태와 히스토리가 보존된다.

---

### User Story 5 - Structured Feedback And Test History (Priority: P2)

테스터 팀은 테스트 종료 후 구조화된 피드백을 남기고, 시스템은 완료된 테스트와 피드백 이력을
할당 단위로 보존한다.

**Why this priority**: 완료된 테스트와 피드백을 자산으로 누적해 `Match`의 장기 가치를 만든다.

**Independent Test**: 수락된 테스트 할당에서 피드백을 제출하고, 요청 팀과 대상 팀이 할당 상세와
팀별 테스트 이력에서 해당 피드백을 조회할 수 있는지 검증한다.

**Acceptance Scenarios**:

1. **Given** 수락된 테스트 할당이 있을 때, **When** 테스터 팀 MEMBER가 항목별 점수와 자유
   서술을 제출하면, **Then** 피드백은 해당 할당에 연결되어 저장된다.
2. **Given** 피드백 제출이 완료되었을 때, **When** 요청 팀 또는 대상 팀이 할당 상세를 조회하면,
   **Then** 제출된 점수와 서술, 제출 시각, 제출 팀이 표시된다.
3. **Given** 팀에 완료된 테스트 할당이 여러 개 있을 때, **When** 팀별 테스트 이력을 조회하면,
   **Then** 완료된 할당과 피드백 요약이 최신순으로 반환된다.

---

### User Story 6 - Service Pivot And Team Administration (Priority: P3)

팀 OWNER는 서비스 피봇에 맞춰 서비스 프로필을 교체하고, 예외적인 팀 구성 변경을 처리할 수 있다.

**Why this priority**: 소마 팀의 피봇과 팀원 변동을 제품 생애주기 안에서 흡수한다.

**Independent Test**: OWNER가 새 서비스 프로필을 active로 지정하고 기존 프로필 이력이 보존되는지,
팀원 제거 또는 역할 변경이 권한과 히스토리에 반영되는지 검증한다.

**Acceptance Scenarios**:

1. **Given** 기존 active 서비스 프로필이 있을 때, **When** OWNER가 새 프로필을 active로 지정하면,
   **Then** 기존 프로필은 이력으로 보존되고 새 프로필만 현재 프로필로 사용된다.
2. **Given** 팀 구성 변경이 필요한 상황일 때, **When** OWNER가 팀원을 제거하거나 역할을
   변경하면, **Then** 권한은 즉시 반영되고 변경 히스토리가 남는다.

### Edge Cases

- 유효하지 않거나 만료된 초대 코드로 팀 합류를 요청하는 경우
- 이미 팀에 속한 사용자가 다른 팀 생성 또는 합류를 요청하는 경우
- 한 팀이 `Calendar`만, `Match`만, 또는 둘 다 활성화하는 경우
- OWNER가 없는 팀이 생길 수 있는 팀원 제거 또는 역할 변경을 시도하는 경우
- 같은 멘토링 일정을 여러 번 bulk push하는 경우
- Google Calendar 연동 권한이 없거나 만료된 상태에서 캘린더 기능을 요청하는 경우
- When2meet 링크가 허용되지 않은 도메인이거나 파싱할 수 없는 HTML 구조인 경우
- 마감된 캠페인 또는 비공개 캠페인에 요청을 보내는 경우
- 자기 팀의 캠페인에 베타 요청을 보내는 경우
- 동일 캠페인에 같은 팀이 중복 요청을 보내는 경우
- 요청이 취소 또는 거절된 뒤 수락을 시도하는 경우
- 할당되지 않은 팀이 피드백을 제출하는 경우
- 완료된 테스트 이력이 아직 없는 경우
- 다른 팀의 비공개 데이터에 접근하는 경우

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 시스템은 소마 내부 인증 사용자만 보호된 기능에 접근하도록 해야 한다.
- **FR-002**: 시스템은 팀 생성 시 필수 팀 프로필 값을 검증하고 OWNER 멤버십과 초대 코드를
  함께 생성해야 한다.
- **FR-003**: 시스템은 초대 코드 기반 팀 합류를 지원하고, 한 사용자가 하나의 팀에만 속하도록
  보장해야 한다.
- **FR-004**: 시스템은 OWNER와 MEMBER 역할을 구분하고, 팀 스코프 권한을 모든 팀 데이터 접근에
  적용해야 한다.
- **FR-005**: 시스템은 팀별로 `Calendar`와 `Match` sub-service를 독립적으로 활성화하거나
  비활성화할 수 있어야 한다.
- **FR-006**: 시스템은 상태 변화가 필요한 주요 이벤트에 대해 인앱 알림을 생성해야 한다.
- **FR-007**: 시스템은 `Calendar` 활성화 팀에 대해 팀 캘린더 연결 상태를 관리해야 한다.
- **FR-008**: 시스템은 소마 멘토링 일정 목록을 팀 캘린더에 bulk push할 수 있어야 한다.
- **FR-009**: 시스템은 멘토링 일정 bulk push 시 동일 일정 중복 생성을 방지해야 한다.
- **FR-010**: 시스템은 팀별 When2meet 링크 등록, 교체, 제거를 지원해야 한다.
- **FR-011**: 시스템은 유효한 When2meet 링크를 파싱해 팀 가용성 슬롯으로 정규화해야 한다.
- **FR-012**: 시스템은 Google Calendar 일정 상태와 When2meet 가용성 슬롯을 병합한 통합
  가용성 뷰를 제공해야 한다.
- **FR-013**: 시스템은 When2meet 파싱 실패 시 실패 사유와 원본 링크 fallback 정보를 팀에
  제공해야 한다.
- **FR-014**: 시스템은 `Match` 활성화 팀이 서비스명, 한 줄 소개, 상세, 플랫폼, 스크린샷,
  데모 링크를 포함한 서비스 프로필을 등록하고 공개할 수 있게 해야 한다.
- **FR-015**: 시스템은 한 팀이 하나의 active 서비스 프로필만 갖도록 하되, 이전 프로필 이력을
  보존해야 한다.
- **FR-016**: 시스템은 active 서비스 프로필을 가진 팀이 베타 캠페인을 생성, 수정, 공개, 비공개,
  종료할 수 있게 해야 한다.
- **FR-017**: 시스템은 캠페인에 모집 팀 수, 마감일, 맞베타 가능 여부, 요구 사항을 저장해야 한다.
- **FR-018**: 시스템은 공개 캠페인 탐색, 검색, 필터, 정렬을 제공해야 한다.
- **FR-019**: 시스템은 다른 팀 캠페인에 단방향 베타 요청과 맞베타 요청을 보낼 수 있게 해야 한다.
- **FR-020**: 시스템은 요청 상태를 pending, accepted, rejected, canceled 중 하나로 관리하고
  상태 변경 히스토리를 보존해야 한다.
- **FR-021**: 시스템은 요청 수락 시 테스트 할당을 자동 생성해야 한다.
- **FR-022**: 시스템은 테스트 할당에 대해 항목별 점수와 자유 서술을 포함한 구조화된 피드백
  제출을 지원해야 한다.
- **FR-023**: 시스템은 피드백 제출 완료 시 테스트 할당 상태와 완료 이력을 갱신해야 한다.
- **FR-024**: 시스템은 팀별 테스트 할당 및 피드백 이력 조회를 지원해야 한다.
- **FR-025**: 시스템은 팀원 제거, 역할 변경, OWNER 이관을 지원하고 변경 히스토리를 보존해야 한다.
- **FR-026**: 시스템은 공개 설정이 아닌 다른 팀의 캠페인, 요청, 피드백, 가용성 데이터를 노출하지
  않아야 한다.

### Non-Functional Requirements

- **NFR-001**: 통합 가용성 뷰는 정상 조건에서 3초 이내에 응답해야 한다.
- **NFR-002**: 모든 보호된 API는 미인증 요청에 대해 401을 반환해야 한다.
- **NFR-003**: 시스템은 미인증 요청 5 req/s, 인증 요청 20 req/s 수준의 rate limit 정책을
  적용할 수 있어야 한다.
- **NFR-004**: 외부 URL 파싱은 허용된 When2meet 도메인으로 제한해야 한다.
- **NFR-005**: OAuth Client Secret, Refresh Token, 암호화 키는 클라이언트에 노출되지 않아야 한다.
- **NFR-006**: Google Calendar 권한은 `Calendar`를 사용하는 팀에만 요청되어야 한다.
- **NFR-007**: Boolean 응답 필드는 null을 허용하지 않아야 하며, 복수 응답의 빈 값은 빈 배열로
  반환해야 한다.

### Key Entities *(include if feature involves data)*

- **User**: 소마 내부 인증 사용자. 한 사용자는 최대 하나의 팀에 속한다.
- **Team**: `Calendar`와 `Match`의 공통 루트. 팀 프로필, 초대 코드, sub-service 활성화 상태를 가진다.
- **TeamMember**: 사용자와 팀의 멤버십. OWNER 또는 MEMBER 역할과 변경 이력을 가진다.
- **SubServiceActivation**: 팀별 `Calendar`, `Match` 활성화 상태.
- **TeamCalendar**: 팀의 캘린더 연결 정보와 Calendar 기능 상태.
- **MentoringSchedule**: 소마 멘토링 일정. 중복 방지를 위한 외부 식별 정보를 포함한다.
- **When2meetLink**: 팀이 등록한 When2meet 원본 링크와 파싱 상태.
- **AvailabilitySlot**: When2meet 또는 Calendar에서 도출된 시간대별 가용성 정보.
- **UnifiedAvailability**: 여러 소스의 가용성을 병합한 조회 모델.
- **ServiceProfile**: 팀이 공개하는 서비스 소개. 팀당 하나의 active 프로필과 과거 버전이 존재한다.
- **BetaCampaign**: 서비스 프로필에 연결된 베타 모집 단위. 모집 조건과 공개 상태를 가진다.
- **MatchRequest**: 단방향 또는 맞베타 요청. 상태와 히스토리를 가진다.
- **Assignment**: 수락된 요청에서 생성되는 테스트 수행 단위.
- **Feedback**: 테스트 종료 후 제출되는 항목별 점수와 자유 서술.
- **Notification**: 요청, 수락, 거절, 취소, 피드백 요청 등 상태 변화 알림.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 신규 OWNER는 팀 생성, 초대 코드 발급, 하나 이상의 sub-service 활성화를 5분 이내에
  완료할 수 있다.
- **SC-002**: 동일 멘토링 일정 목록을 2회 이상 bulk push해도 팀 캘린더에 중복 일정이 생성되지 않는다.
- **SC-003**: 통합 가용성 뷰는 초기 MVP 기준 100팀 규모에서 정상 조건 p95 3초 이내에 응답한다.
- **SC-004**: MEMBER는 카테고리, 플랫폼, 맞베타 가능 여부 필터를 조합해 공개 캠페인을 조회할 수 있다.
- **SC-005**: 요청 수락 후 테스트 할당과 알림은 사용자 재시도 없이 생성된다.
- **SC-006**: 피드백 제출 후 할당 상세와 팀별 테스트 이력에 피드백 요약이 반영된다.
- **SC-007**: 다른 팀의 비공개 요청, 피드백, 가용성 데이터 접근 시도는 모두 거부된다.
- **SC-008**: Should Have 범위인 서비스 프로필 교체와 팀원 관리는 기존 캠페인, 요청, 피드백 이력을
  삭제하지 않고 수행된다.

## Assumptions

- 본 spec은 PRD 전체를 MVP 상위 feature로 정리한 문서이며, 구현 계획 단계에서 `common`,
  `calendar`, `match` 세부 feature로 나눌 수 있다.
- OAuth 로그인은 Google OAuth 2.0 Authorization Code Flow를 사용한다.
- 소마 내부 사용자 여부를 판별하는 구체 기준은 별도 인증 정책에서 확정한다.
- 웹 프론트와 크롬 익스텐션은 본 spec의 API 계약을 사용하는 클라이언트로 다룬다.
- 네이티브 모바일 앱, 실시간 채팅, 결제, 추천 알고리즘 고도화는 MVP 범위에서 제외한다.

## Open Questions

- **OQ-001**: 소마 내부 사용자 판별은 이메일 도메인, 사전 등록 명단, 또는 별도 인증 소스 중
  어떤 기준으로 확정할 것인가?
- **OQ-002**: 캠페인 카테고리와 플랫폼 enum의 초기 값은 무엇인가?
- **OQ-003**: 피드백 항목별 점수의 항목명과 척도는 어떻게 정의할 것인가?
- **OQ-004**: `Calendar` 비활성화 시 기존 캘린더 연결 데이터와 When2meet 파싱 결과를 보존할 것인가,
  숨김 처리할 것인가, 삭제할 것인가?
- **OQ-005**: OWNER와 MEMBER가 수정할 수 있는 팀 프로필, 서비스 프로필, 캠페인, 캘린더 설정의
  정확한 권한 매트릭스는 어떻게 정의할 것인가?

## PRD Traceability

| PRD Story | Covered By |
|-----------|------------|
| US-01, US-02 | User Story 1 |
| US-03, US-04, US-05 | User Story 2 |
| US-06, US-07, US-08 | User Story 3 |
| US-09, US-10, US-14 | User Story 4 |
| US-11 | User Story 5 |
| US-12 | Not covered by this spec |
| US-13, US-15 | User Story 6 |
