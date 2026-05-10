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
    add("implementation", project(":core:core-enum"))
    add("implementation", project(":support:monitoring"))
    add("implementation", project(":support:logging"))
    add("implementation", project(":storage:db-core"))
    add("implementation", project(":clients:client-example"))

    add("testImplementation", project(":tests:api-docs"))

    add("implementation", "org.springframework.boot:spring-boot-h2console")
    add("implementation", "org.springframework.boot:spring-boot-starter-restclient")
    add("implementation", "org.springframework.boot:spring-boot-starter-security-oauth2-client")
    add("implementation", "org.springframework.boot:spring-boot-starter-validation")
    add("implementation", "org.springframework.boot:spring-boot-starter-webmvc")
    add("implementation", "com.fasterxml.jackson.core:jackson-databind")
}
