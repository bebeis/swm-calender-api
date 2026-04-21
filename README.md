# swm-teams

소마(소프트웨어 마에스트로) 내부 팀들을 위한 통합 제품. `swm-teams`가 루트이고, 그 아래에 두 개의 sub-service가 독립적으로 존재한다.

```
swm-teams (root)
├── Calendar   — 팀 일정 통합 (Google Calendar + When2meet)
└── Match      — 서비스 홍보 + 맞베타 매칭 + 관계 그래프
```

팀은 `Calendar`와 `Match` 중 한쪽만, 또는 둘 다 선택적으로 활성화해 사용할 수 있다.

> 멘토링 일정 싱크 전용이었던 `swm-calender`는 `Calendar` sub-service로 이전·확장되었고,
> `Match`는 이번 피봇에서 신규로 추가된 sub-service다.

## Sub-services

### `Calendar` — 팀 일정 통합

- Google Calendar 팀 캘린더 자동 생성
- 소마 멘토링 → 팀 캘린더 원클릭/일괄 등록
- When2meet 링크 서버 파싱
- Google `freebusy` + When2meet을 합친 통합 가용성 뷰

**해결하는 문제**
- 멘토링 일정을 팀 Google Calendar에 수동 복제하면 누락이 잦다.
- When2meet 같은 외부 도구의 가용성이 팀 도메인과 단절되어 있다.

### `Match` — 팀 서비스 홍보 + 맞베타 매칭

- 팀 프로필 및 서비스 프로필 (피봇 대응 버저닝)
- 베타 캠페인 생성·수정·오픈·종료
- 캠페인 탐색·검색·필터
- 단방향 요청 / **맞베타** 요청, 수락·거절·취소
- 테스트 할당 관리 및 구조화된 피드백
- 내 팀 중심 / 전체 팀 관계 그래프
- 인앱 알림

**해결하는 문제**
- 홍보 글이 슬랙/디스코드/노션/단톡에 분산되어 나중에 찾기 어렵다.
- "우리도 해줄게, 너희도 해줘" 식의 맞베타 협의가 DM에 흩어져 히스토리가 남지 않는다.
- 누가 우리 팀을 테스트했고, 우리가 누구를 도왔는지 한눈에 보이지 않는다.

## 페르소나

- **팀 OWNER**: 팀/서비스/캠페인/캘린더 관리, 들어온 요청 수락·거절
- **팀 MEMBER**: 다른 팀 탐색, 요청 송신, 테스트 수행, 피드백 작성, 팀 일정 조회
- **커뮤니티 전체**: 어떤 팀이 활발히 베타를 교환하는지 그래프로 파악

## 도메인 규칙

- 서비스는 **소마 내부 사용자 전용**
- 한 팀은 **하나의 서비스(active)**만 보유 (`Match` 범위, 피봇 시 교체/버전 관리)
- 한 사용자는 **하나의 팀**에만 소속
- 팀 구성은 특수한 경우(탈주 등)에 한해 변경 가능
- 팀은 sub-service를 선택적으로 활성화할 수 있음

## 기술 스택

- Kotlin / Spring Boot 4.0.x
- JDK 21
- MySQL 8.0 (prod) / H2 (local, test)
- Terraform, AWS EC2, RDS, ECR, CloudWatch
- GitHub Actions

## 빌드 & 실행

```bash
./gradlew build        # 빌드
./gradlew bootRun      # 실행
./gradlew test         # 전체 테스트
./gradlew clean build  # 클린 빌드
```

## 문서

- PRD: [`docs/v1/plan/PRD.md`](docs/v1/plan/PRD.md)
- 코드 아키텍처 가이드: [`docs/code-style/CODE_ARCHITECTURE_GUIDE.md`](docs/code-style/CODE_ARCHITECTURE_GUIDE.md)
- IaC: [`iac/README.md`](iac/README.md)
