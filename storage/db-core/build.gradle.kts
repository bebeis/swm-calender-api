import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension

apply(plugin = "org.jetbrains.kotlin.kapt")

extensions.configure<AllOpenExtension> {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    add("implementation", "org.springframework.boot:spring-boot-starter-data-jpa")
    add("implementation", "org.springframework.boot:spring-boot-starter-flyway")
    add("implementation", "com.querydsl:querydsl-jpa:${property("querydslVersion")}:jakarta")
    add("kapt", "com.querydsl:querydsl-apt:${property("querydslVersion")}:jakarta")
    add("kapt", "jakarta.persistence:jakarta.persistence-api")
    add("kapt", "jakarta.annotation:jakarta.annotation-api")
    add("runtimeOnly", "org.flywaydb:flyway-mysql")
    add("runtimeOnly", "com.mysql:mysql-connector-j")
    add("runtimeOnly", "com.h2database:h2")
}
