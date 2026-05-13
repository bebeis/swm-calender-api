# Web

SWM Teams MVP 웹 프론트엔드다. 별도 번들러 없이 `index.html`을 여는 정적 앱으로
시작하며, 백엔드 API 계약은 `packages/api-client`의 공유 클라이언트를 사용한다.

## 실행

```bash
npm run web:dev
```

브라우저에서 `http://localhost:3000/apps/web/`을 연다. 백엔드 기본 CORS 설정이
`http://localhost:3000`을 허용하므로 API 호출까지 확인하려면 정적 서버로 실행해야 한다.

보호 API 호출에는 백엔드 API base URL과 bearer access token이 필요하다. access token은
브라우저 저장소에 저장하지 않고 현재 페이지 세션 메모리에만 유지한다.

## 포함된 MVP 화면

- 팀 생성, 초대 코드 합류, Calendar/Match 활성화, 멤버 관리
- When2meet 링크 등록, 멘토링 일정 push, 통합 가용성 조회
- 서비스 프로필 공개, 캠페인 생성/탐색, 후보 아이디어 생성, 중복 분석 실행
- 베타 요청 생성/상태 변경, 할당 상세 조회, 피드백 제출, 테스트 이력 조회

## 검증

```bash
npm run frontend:check
```
