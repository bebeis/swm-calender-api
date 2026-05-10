tasks.named("bootRun").configure {
    enabled = true
}

tasks.named("bootJar").configure {
    enabled = true
}

tasks.named("jar").configure {
    enabled = false
}

dependencies {
    add("implementation", project(":core:core-common"))
    add("implementation", project(":core:core-enum"))
    add("implementation", project(":core:core-team-domain"))
    add("implementation", project(":calendar:calendar-domain"))
    add("implementation", project(":match:match-domain"))
    add("implementation", project(":support:monitoring"))
    add("implementation", project(":support:logging"))
    add("implementation", project(":clients:google-calendar"))
    add("implementation", project(":clients:when2meet"))
    add("implementation", project(":clients:client-example"))
    add("runtimeOnly", project(":storage:db-core"))

    add("testImplementation", project(":tests:api-docs"))

    add("implementation", "org.springframework.boot:spring-boot-h2console")
    add("implementation", "org.springframework.boot:spring-boot-starter-restclient")
    add("implementation", "org.springframework.boot:spring-boot-starter-security-oauth2-client")
    add("implementation", "org.springframework.boot:spring-boot-starter-validation")
    add("implementation", "org.springframework.boot:spring-boot-starter-webmvc")
    add("implementation", "com.fasterxml.jackson.core:jackson-databind")
}
