import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") apply false
    kotlin("plugin.spring") apply false
    kotlin("plugin.jpa") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
    id("org.asciidoctor.jvm.convert") apply false
    id("org.jlleitschuh.gradle.ktlint") apply false
}

allprojects {
    group = property("projectGroup").toString()
    version = property("applicationVersion").toString()

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.asciidoctor.jvm.convert")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(property("javaVersion").toString().toInt())
        }
    }

    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
    }

    extensions.configure<DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudDependenciesVersion")}")
        }
    }

    dependencies {
        add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
        add("implementation", "com.fasterxml.jackson.module:jackson-module-kotlin")
        add("annotationProcessor", "org.springframework.boot:spring-boot-configuration-processor")
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5")
        add("testImplementation", "org.springframework.restdocs:spring-restdocs-mockmvc")
        add("testImplementation", "com.ninja-squad:springmockk:${property("springMockkVersion")}")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.named("bootRun").configure {
        enabled = false
    }

    tasks.named("bootJar").configure {
        enabled = false
    }

    tasks.named("jar").configure {
        enabled = true
    }
}
