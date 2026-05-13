# Extension

SWM Teams Chrome extension MVP다. `swmaestro.ai` 멘토링/특강 접수 내역 페이지에 content script를
주입해, 페이지 안에서 신청 내역을 팀 캘린더 API로 보낼 수 있게 한다.

## Surface

- 대상 페이지:
  - `https://swmaestro.ai/sw/mypage/userAnswer/history.do*`
  - `https://www.swmaestro.ai/sw/mypage/userAnswer/history.do*`
  - `https://swmaestro.ai/busan/sw/mypage/userAnswer/history.do*`
  - `https://www.swmaestro.ai/busan/sw/mypage/userAnswer/history.do*`
- content script:
  - 접수 완료된 멘토링/특강 목록을 파싱한다.
  - 신청 내역 페이지 내부에 `SWM Teams Calendar` 패널을 삽입한다.
  - 선택한 멘토링 일정을 `POST /api/v1/calendar/mentoring-schedules:bulk-push`로 보낸다.
  - When2meet URL을 `PUT /api/v1/calendar/when2meet-link`로 등록한다.
  - 선택 일정 범위의 팀 가용성을 `GET /api/v1/calendar/availability`로 조회한다.
- popup:
  - 서울/부산 접수 내역 페이지 링크를 제공한다.
  - content script가 사용할 API origin만 저장한다. 기본값은 `http://localhost:8080`이다.

## Architecture

```text
swmaestro.ai history page
  └─ src/history-content.js
      ├─ DOM에서 신청 내역 파싱
      ├─ 페이지 내부 패널 렌더링
      ├─ When2meet URL 등록
      └─ chrome.runtime.sendMessage(...)

src/background.js
  └─ vendor/swm-api-client.js로 백엔드 API 호출
```

API 호출은 content script가 직접 하지 않고 background service worker가 수행한다. 이렇게 하면
`swmaestro.ai` 페이지의 CORS 정책과 분리되고, manifest의 `host_permissions`가 API 호출 권한 경계가 된다.
로컬 MVP는 `http://localhost:8080` 백엔드를 대상으로 한다. 배포 API를 연결하려면 manifest의
`host_permissions`, popup 안내, 검증 스크립트를 함께 갱신한다.

## Permissions

- `storage`: API origin 설정 저장
- `host_permissions`:
  - `http://localhost:8080/*`

토큰, refresh token, Google OAuth credential, backend secret은 저장하지 않는다. Bearer access token은
신청 내역 페이지에 삽입된 패널의 메모리에만 유지되며 페이지를 닫거나 새로고침하면 사라진다.
`swmaestro.ai` 페이지 접근은 `content_scripts.matches`로만 제한하고, background API 호출 권한은
로컬 백엔드 origin에만 부여한다.

## 로컬 로드

Chrome의 `chrome://extensions`에서 Developer mode를 켠 뒤 `apps/extension` 디렉토리를
Load unpacked로 선택한다.

## 공유 클라이언트

Chrome extension은 manifest root 밖의 파일을 직접 로드할 수 없어서
`packages/api-client/src/swm-api-client.js`를 `vendor/swm-api-client.js`에 동기화해 사용한다.
`npm run frontend:check`는 두 파일 동기화, Manifest V3 구조, content script match, permission 범위를
검증한다.
