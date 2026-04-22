dependencies {
    add("implementation", "org.springframework.boot:spring-boot-starter-opentelemetry")
    add("implementation", "io.sentry:sentry-logback:${property("sentryVersion")}")
}
