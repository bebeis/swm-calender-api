# Observability Pipeline

`swm-teams`는 아래 파이프라인을 기준으로 운영합니다.

```text
Application
├─ Metrics -> /actuator/prometheus -> Prometheus -> Grafana
├─ Traces  -> OTLP/HTTP -> Tempo -> Grafana
├─ Logs    -> stdout -> Promtail or Grafana Alloy -> Loki -> Grafana
└─ Errors  -> Slack webhook
```

## 선택한 구조

- 메트릭은 애플리케이션이 Prometheus 형식으로 노출하고, Prometheus가 scrape 합니다.
- 트레이스는 애플리케이션이 OTLP로 Tempo에 직접 전송합니다.
- 로그는 애플리케이션이 stdout으로만 출력하고, 수집기는 Promtail 또는 Grafana Alloy가 맡습니다.
- Slack webhook은 로그 저장소 대체제가 아니라 예외/에러 알림 채널입니다.

이 구조에서는 애플리케이션이 Loki에 직접 push 하지 않습니다. 로그 수집 책임은 인프라에 둡니다.

## 모듈별 역할

| 모듈 | 직접 의존성 | 역할 |
| --- | --- | --- |
| `support:monitoring` | `spring-boot-starter-actuator`, `micrometer-registry-prometheus` | Prometheus scrape 대상 제공 |
| `support:logging` | `spring-boot-starter-opentelemetry` | OTLP trace export, stdout logging, Slack 에러 알림 |
| `core:core-api` | `:support:monitoring`, `:support:logging` | 애플리케이션 진입점, actuator endpoint 노출 |

## 애플리케이션 책임

### 1. Metrics

- `/actuator/prometheus` endpoint를 노출합니다.
- Prometheus가 이 endpoint를 주기적으로 scrape 합니다.
- 애플리케이션은 OTLP metrics export를 사용하지 않습니다.

관련 파일:
- [support/monitoring/build.gradle.kts](/Users/luna/Desktop_nonsync/project/swm-calender/apps/backend/support/monitoring/build.gradle.kts)
- [support/monitoring/src/main/resources/monitoring.yml](/Users/luna/Desktop_nonsync/project/swm-calender/apps/backend/support/monitoring/src/main/resources/monitoring.yml)
- [apps/backend/core/core-api/src/main/resources/application.yml](/Users/luna/Desktop_nonsync/project/swm-calender/apps/backend/core/core-api/src/main/resources/application.yml)

### 2. Traces

- `spring-boot-starter-opentelemetry`를 통해 Micrometer Observation 기반 trace를 생성합니다.
- trace는 `management.opentelemetry.tracing.export.otlp.*` 설정으로 Tempo에 전송합니다.
- 기본 transport는 `http/protobuf`입니다.

관련 파일:
- [support/logging/build.gradle.kts](/Users/luna/Desktop_nonsync/project/swm-calender/apps/backend/support/logging/build.gradle.kts)
- [support/logging/src/main/resources/logging.yml](/Users/luna/Desktop_nonsync/project/swm-calender/apps/backend/support/logging/src/main/resources/logging.yml)

### 3. Logs

- 운영 프로필에서는 콘솔 로그를 구조화된 JSON으로 stdout에 기록합니다.
- 로그 수집기는 stdout를 읽어 Loki로 전달합니다.
- `traceId`, `spanId`는 MDC에 들어가므로 로그와 trace를 Grafana에서 연결할 수 있습니다.

관련 파일:
- [support/logging/src/main/resources/logback/logback-dev.xml](/Users/luna/Desktop_nonsync/project/swm-calender/apps/backend/support/logging/src/main/resources/logback/logback-dev.xml)
- [support/logging/src/main/resources/logback/logback-prod.xml](/Users/luna/Desktop_nonsync/project/swm-calender/apps/backend/support/logging/src/main/resources/logback/logback-prod.xml)

### 4. Errors

- `WARN` 이상 이벤트는 Slack webhook appender로 전송할 수 있습니다.
- 실제 전송 여부는 `SLACK_ERROR_WEBHOOK_URL` 설정에 달려 있습니다.

관련 파일:
- [support/logging/src/main/resources/logback/logback-dev.xml](/Users/luna/Desktop_nonsync/project/swm-calender/apps/backend/support/logging/src/main/resources/logback/logback-dev.xml)
- [support/logging/src/main/resources/logback/logback-prod.xml](/Users/luna/Desktop_nonsync/project/swm-calender/apps/backend/support/logging/src/main/resources/logback/logback-prod.xml)

## 필요한 환경 변수

| 변수 | 예시 | 설명 |
| --- | --- | --- |
| `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | `http://tempo:4318/v1/traces` | Tempo OTLP HTTP ingest endpoint |
| `OTEL_EXPORTER_OTLP_TRACES_TRANSPORT` | `http/protobuf` | trace export transport |
| `TRACING_SAMPLING_PROBABILITY` | `1.0` | trace 샘플링 비율 |
| `CONSOLE_LOG_STRUCTURED_FORMAT` | `logstash` | stdout JSON 포맷 |
| `SLACK_ERROR_WEBHOOK_URL` | `https://hooks.slack.com/services/...` | Slack Incoming Webhook URL |
| `SLACK_ERROR_MIN_LEVEL` | `WARN` | Slack 알림 최소 로그 레벨 |

## 인프라 책임

### Prometheus

- `core-api`의 `/actuator/prometheus`를 scrape 합니다.
- 기본적으로 `job`, `instance`, `application` 라벨을 붙여 Grafana에서 조회합니다.

### Tempo

- OTLP HTTP endpoint를 노출해야 합니다.
- 애플리케이션은 기본값으로 `http://localhost:4318/v1/traces`를 사용하므로 운영 환경에서는 실제 Tempo 주소로 덮어써야 합니다.

### Promtail 또는 Grafana Alloy

- 애플리케이션 stdout를 수집합니다.
- `traceId`, `spanId`, `level`, `logger_name`, `service` 같은 필드를 Loki label 또는 parsed field로 넘깁니다.
- Docker 환경이면 container stdout를 읽고, systemd 환경이면 journald 또는 파일을 읽도록 설정합니다.

### Loki

- 수집기에서 전달한 애플리케이션 로그를 저장합니다.
- Grafana Explore에서 traceId 기준으로 로그를 검색할 수 있게 구성합니다.

### Grafana

- Prometheus, Tempo, Loki data source를 연결합니다.
- 대시보드에서는 request latency, error rate, trace waterfall, application logs를 함께 봅니다.

## 운영 메모

- 메트릭은 Prometheus scrape 방식이므로 애플리케이션에서 OTLP metrics exporter를 켜지 않습니다.
- 로그는 Loki direct appender 대신 stdout 수집 방식을 유지합니다.
- `prod` 프로필은 [logback-prod.xml](/Users/luna/Desktop_nonsync/project/swm-calender/apps/backend/support/logging/src/main/resources/logback/logback-prod.xml)을 사용합니다.
- 테스트에서는 tracing export를 끄도록 [apps/backend/core/core-api/src/test/resources/application.yml](/Users/luna/Desktop_nonsync/project/swm-calender/apps/backend/core/core-api/src/test/resources/application.yml)을 분리해 두었습니다.

## 참고

- Spring Boot Observability: https://docs.spring.io/spring-boot/4.0/reference/actuator/observability.html
- Spring Boot Tracing: https://docs.spring.io/spring-boot/reference/actuator/tracing.html
- Spring Boot Logging: https://docs.spring.io/spring-boot/4.0-SNAPSHOT/reference/features/logging.html
