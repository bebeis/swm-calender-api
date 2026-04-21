import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension

extensions.configure<AllOpenExtension> {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    add("implementation", "org.springframework.boot:spring-boot-starter-data-jpa")
    add("runtimeOnly", "com.mysql:mysql-connector-j")
    add("runtimeOnly", "com.h2database:h2")
}
