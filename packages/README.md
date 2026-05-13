# Packages

여러 앱이 공유하는 프론트엔드/공통 패키지를 둔다.

예시:

```text
packages/
├── api-client/
├── config/
└── ui/
```

## `@swm-teams/api-client`

웹 앱과 브라우저 확장이 함께 사용하는 브라우저용 API 클라이언트다. 백엔드의
`ApiResponse<T>` envelope, bearer auth 헤더, `/api/v1` endpoint path를 한 곳에서
처리한다.

현재 프론트엔드는 별도 번들러 없이 실행되므로 `packages/api-client/src/swm-api-client.js`가
`globalThis.SwmApi`를 노출한다. Chrome extension은 manifest root 밖의 파일을 직접 로드할 수
없어서 동일 파일을 `apps/extension/vendor/swm-api-client.js`에 동기화해 사용한다.
