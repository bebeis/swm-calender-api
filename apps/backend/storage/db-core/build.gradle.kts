dependencies {
    add("implementation", platform("org.jetbrains.exposed:exposed-bom:${property("exposedVersion")}"))
    add("implementation", "org.springframework.boot:spring-boot-starter-jdbc")
    add("implementation", "org.springframework.boot:spring-boot-starter-flyway")
    add("implementation", "org.jetbrains.exposed:exposed-core")
    add("implementation", "org.jetbrains.exposed:exposed-jdbc")
    add("implementation", "org.jetbrains.exposed:exposed-java-time")
    add("implementation", "org.jetbrains.exposed:exposed-spring-boot4-starter")
    add("runtimeOnly", "org.flywaydb:flyway-mysql")
    add("runtimeOnly", "com.mysql:mysql-connector-j")
    add("runtimeOnly", "com.h2database:h2")
}
