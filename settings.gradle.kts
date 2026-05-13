rootProject.name = "swm-teams"

include(
    "core:core-common",
    "core:core-enum",
    "core:core-team-domain",
    "core:core-api",
    "calendar:calendar-domain",
    "match:match-domain",
    "storage:db-core",
    "tests:api-docs",
    "support:logging",
    "support:monitoring",
    "clients:client-example",
    "clients:google-calendar",
    "clients:gemini",
    "clients:when2meet",
)

project(":core").projectDir = file("apps/backend/core")
project(":core:core-common").projectDir = file("apps/backend/core/core-common")
project(":core:core-enum").projectDir = file("apps/backend/core/core-enum")
project(":core:core-team-domain").projectDir = file("apps/backend/core/core-team-domain")
project(":core:core-api").projectDir = file("apps/backend/core/core-api")
project(":calendar").projectDir = file("apps/backend/calendar")
project(":calendar:calendar-domain").projectDir = file("apps/backend/calendar/calendar-domain")
project(":match").projectDir = file("apps/backend/match")
project(":match:match-domain").projectDir = file("apps/backend/match/match-domain")
project(":storage").projectDir = file("apps/backend/storage")
project(":storage:db-core").projectDir = file("apps/backend/storage/db-core")
project(":tests").projectDir = file("apps/backend/tests")
project(":tests:api-docs").projectDir = file("apps/backend/tests/api-docs")
project(":support").projectDir = file("apps/backend/support")
project(":support:logging").projectDir = file("apps/backend/support/logging")
project(":support:monitoring").projectDir = file("apps/backend/support/monitoring")
project(":clients").projectDir = file("apps/backend/clients")
project(":clients:client-example").projectDir = file("apps/backend/clients/client-example")
project(":clients:google-calendar").projectDir = file("apps/backend/clients/google-calendar")
project(":clients:gemini").projectDir = file("apps/backend/clients/gemini")
project(":clients:when2meet").projectDir = file("apps/backend/clients/when2meet")

pluginManagement {
    val kotlinVersion: String by settings
    val springBootVersion: String by settings
    val springDependencyManagementVersion: String by settings
    val asciidoctorConvertVersion: String by settings
    val ktlintVersion: String by settings

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "org.jetbrains.kotlin.jvm" -> useVersion(kotlinVersion)
                "org.jetbrains.kotlin.plugin.spring" -> useVersion(kotlinVersion)
                "org.springframework.boot" -> useVersion(springBootVersion)
                "io.spring.dependency-management" -> useVersion(springDependencyManagementVersion)
                "org.asciidoctor.jvm.convert" -> useVersion(asciidoctorConvertVersion)
                "org.jlleitschuh.gradle.ktlint" -> useVersion(ktlintVersion)
            }
        }
    }
}
