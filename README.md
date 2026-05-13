# swm-teams

소프트웨어 마에스트로 팀을 위한 팀 운영 도구다. 한 팀은 필요한 기능만 선택해서
`Calendar`와 `Match`를 독립적으로 사용할 수 있다.

```text
swm-teams
├── Calendar   - 팀 일정 통합 (Google Calendar + When2meet)
└── Match      - 서비스 홍보 + 맞베타 매칭 + 피드백 기록
```

## 사용자 가이드

### 누가 사용하나

- **팀 OWNER**: 팀 생성, 초대 코드 공유, 팀원 관리, Calendar/Match 활성화, 서비스 프로필과 캠페인 관리,
  들어온 베타 요청 수락/거절
- **팀 MEMBER**: 팀 합류, 팀 일정 확인, 공개 캠페인 탐색, 베타 요청 보내기, 테스트 수행, 피드백 작성

### 처음 시작하기

1. 웹 앱에 접속한다.
   - 로컬 MVP 확인: `npm run web:dev` 실행 후 `http://localhost:3000/apps/web/` 접속
2. API base URL과 bearer access token을 입력한다.
   - 로컬 기본 API: `http://localhost:8080`
   - access token은 현재 브라우저 페이지 메모리에만 유지된다.
3. 팀이 없다면 **팀 만들기**로 팀을 생성한다.
4. 기존 팀에 합류한다면 OWNER에게 받은 **초대 코드**를 입력한다.
5. 팀 화면에서 필요한 sub-service를 켠다.
   - `Calendar`: 팀 일정과 가용성 관리
   - `Match`: 서비스 홍보, 맞베타 요청, 피드백 관리

현재 MVP에는 "내 팀 자동 조회" API가 없어서, 웹 앱은 팀 생성/합류 응답으로 받은 팀을 현재 작업 팀으로
기억한다. 다른 브라우저나 새 환경에서는 다시 팀 생성/합류 흐름을 통해 작업 팀을 잡아야 한다.

## Calendar 사용법

Calendar는 멘토링 일정과 When2meet 가용성을 팀 단위로 합쳐 보는 기능이다.

1. 팀 화면에서 `Calendar`를 활성화한다.
2. Calendar 화면에서 When2meet URL을 등록한다.
   - 허용되는 URL은 `https://when2meet.com/...` 형식이다.
   - 파싱에 실패하면 실패 사유와 원본 링크가 남는다.
3. 멘토링 일정을 입력해 팀 캘린더에 push한다.
   - 같은 외부 일정 ID로 다시 push하면 중복 등록은 스킵된다.
4. 시작/종료 시간을 선택해 통합 가용성을 조회한다.
   - 슬롯별 가능 인원과 바쁜 인원이 함께 표시된다.

## Match 사용법

Match는 팀 서비스 공개, 베타 캠페인 탐색, 맞베타 요청, 피드백 이력을 관리한다.

### 서비스와 캠페인 공개

1. 팀 화면에서 `Match`를 활성화한다.
2. Match 화면에서 서비스 프로필을 작성해 공개한다.
   - 서비스명, 한 줄 소개, 상세 설명, 카테고리, 플랫폼을 입력한다.
3. 베타 캠페인을 만든다.
   - 모집 팀 수, 마감일, 맞베타 가능 여부, 요구 사항을 설정한다.
4. 캠페인 탐색에서 공개 캠페인을 카테고리, 플랫폼, 맞베타 가능 여부로 필터링한다.

### 후보 아이디어와 중복 분석

1. 출시 전 후보 아이디어를 비공개로 저장한다.
2. 후보 아이디어 목록에서 **중복 분석**을 실행한다.
3. 시스템은 공개 서비스와 비공개 후보 아이디어를 함께 비교한다.
4. 다른 팀의 비공개 후보 아이디어와 겹칠 경우 원문, 팀명, 식별자는 노출되지 않고 비식별 요약만 표시된다.

### 베타 요청과 피드백

1. 캠페인 탐색에서 요청할 캠페인을 선택한다.
2. 단방향 요청 또는 맞베타 요청을 보낸다.
3. 대상 팀 OWNER는 요청을 수락, 거절, 취소할 수 있다.
4. 수락된 요청은 테스트 할당으로 이어진다.
5. 테스트 후 점수와 요약, 개선 제안을 피드백으로 제출한다.
6. 피드백과 완료된 테스트는 팀 테스트 이력에서 확인한다.

## Chrome 확장 사용법

Chrome 확장은 `swmaestro.ai` 멘토링/특강 접수 내역 페이지를 팀 캘린더 작업 화면으로 보강한다.

1. Chrome에서 `chrome://extensions`를 연다.
2. Developer mode를 켠다.
3. `apps/extension` 디렉토리를 **Load unpacked**로 선택한다.
4. 확장 popup에서 API origin을 저장한다. 로컬 MVP 기본값은 `http://localhost:8080`이다.
5. 멘토링/특강 접수 내역 페이지로 이동한다.
   - 서울: `https://www.swmaestro.ai/sw/mypage/userAnswer/history.do?menuNo=200047`
   - 부산: `https://www.swmaestro.ai/busan/sw/mypage/userAnswer/history.do?menuNo=200047`
6. 페이지 안에 삽입된 `SWM Teams Calendar` 패널에서 bearer access token을 입력한다.
7. 필요한 경우 When2meet URL을 등록한다.
8. 접수 완료된 멘토링 일정을 선택해 팀 캘린더 API로 push한다.
9. 선택 일정 범위의 팀 가용성을 조회해 Google Calendar/When2meet 기반 팀원 현황을 함께 확인한다.

확장 패널에서 할 수 있는 일:

- SW Maestro 접수 내역 페이지의 멘토링 목록 파싱
- 시간 겹침과 개설 상태 확인
- When2meet URL을 `PUT /api/v1/calendar/when2meet-link`로 등록
- 선택한 멘토링 일정을 `POST /api/v1/calendar/mentoring-schedules:bulk-push`로 전송
- 팀 가용성 슬롯을 `GET /api/v1/calendar/availability`로 조회

확장은 `storage` 권한과 지정된 host permission만 사용한다. 토큰, refresh token, Google OAuth credential,
backend secret은 저장하지 않고 접수 내역 페이지의 content script 메모리에만 유지한다.

## 개인정보와 권한

- 보호된 기능은 인증된 소마 내부 사용자만 사용할 수 있다.
- 한 사용자는 하나의 팀에만 속한다.
- OWNER만 팀 정체성, 팀원 역할, 서비스 프로필, 캠페인 공개 같은 주요 변경을 수행한다.
- 후보 아이디어는 같은 팀 구성원에게만 보인다.
- 다른 팀의 비공개 후보 아이디어는 중복 분석 결과에서도 원문과 식별자가 노출되지 않는다.
- Google OAuth refresh token과 외부 provider credential은 서버에만 보관한다.

## 현재 MVP 제한

- 웹 앱은 아직 정식 로그인 화면이 아니라 API base URL과 bearer token을 직접 입력하는 검증용 UI다.
- 현재 팀을 자동으로 다시 불러오는 API가 없어, 팀 생성/합류 후 받은 응답을 로컬 작업 상태로 사용한다.
- 알림은 목록 조회만 지원한다. 읽음 처리 API는 아직 없다.
- Chrome 확장은 접수 내역 페이지 content script와 API bridge용 background worker만 제공한다.
  background polling, desktop notification은 없다.
- Calendar의 Google Calendar OAuth 연결 화면은 아직 별도 사용자 UI로 분리되어 있지 않다.

## 개발자 실행

### Docker Compose

```bash
docker compose up --build
```

- API: `http://localhost:8080`
- Web: `http://localhost:3000/apps/web/`
- MySQL: `localhost:3306`

Compose는 `.env`가 있으면 같은 키를 사용하고, 없으면 로컬 기본값으로 실행한다.
Gemini 중복 분석을 켜려면 `DUPLICATE_ANALYSIS_PROVIDER=gemini`와 `GEMINI_API_KEY`를 설정한 뒤 실행한다.
호스트 포트는 `API_HOST_PORT`, `WEB_HOST_PORT`, `MYSQL_HOST_PORT`로 바꿀 수 있다.

### 백엔드 API

```bash
./gradlew :core:core-api:bootRun
```

Gemini 기반 후보 아이디어 중복 분석을 사용하려면 API 실행 전에 다음 환경 변수를 설정한다.

```bash
export DUPLICATE_ANALYSIS_PROVIDER=gemini
export GEMINI_API_KEY=<google-ai-studio-api-key>
export GEMINI_MODEL=gemini-2.5-flash
```

### 웹 앱

```bash
npm run web:dev
```

브라우저에서 `http://localhost:3000/apps/web/`을 연다.

### 검증

```bash
./gradlew test
./gradlew ktlintCheck
npm run frontend:check
```

## 기술 스택

- Kotlin / Spring Boot 4.0.x
- JDK 21
- MySQL 8.0 (prod) / H2 (local, test)
- Exposed / Flyway
- Static web app / Chrome Manifest V3 extension
- Terraform, AWS EC2, RDS, ECR, CloudWatch
- GitHub Actions

## Monorepo 구조

```text
swm-teams
├── apps/
│   ├── backend/    # Kotlin/Spring Boot API 서버
│   ├── web/        # 웹 프론트엔드
│   └── extension/  # Chrome 확장 프론트엔드
├── packages/       # 앱 간 공유 패키지
├── docs/
├── iac/
└── specs/
```

백엔드는 `apps/backend` 아래에 모여 있지만 Gradle 프로젝트 경로는 기존처럼
`:core:core-api`, `:storage:db-core`, `:support:logging` 등을 유지한다.

## 문서

- PRD: [`docs/v1/plan/PRD.md`](docs/v1/plan/PRD.md)
- 코드 아키텍처 가이드: [`docs/code-style/CODE_ARCHITECTURE_GUIDE.md`](docs/code-style/CODE_ARCHITECTURE_GUIDE.md)
- 모듈 구조: [`docs/module-structure/README.md`](docs/module-structure/README.md)
- MVP quickstart: [`specs/001-swm-teams-mvp/quickstart.md`](specs/001-swm-teams-mvp/quickstart.md)
- IaC: [`iac/README.md`](iac/README.md)
