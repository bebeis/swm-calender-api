package swm.calender.core.api.controller.v1

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldStartWith
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import swm.calender.core.api.controller.v1.team.TeamController

class ApiVersioningTest :
    FunSpec({
        test("team API mappings use the versioned base path") {
            val mappings = TeamController::class.java.declaredMethods
                .flatMap { method ->
                    listOfNotNull(
                        method.getAnnotation(GetMapping::class.java)?.value?.toList(),
                        method.getAnnotation(PostMapping::class.java)?.value?.toList(),
                        method.getAnnotation(PatchMapping::class.java)?.value?.toList(),
                    ).flatten()
                }

            mappings.shouldNotBeEmpty()
            mappings.forEach { it.shouldStartWith("/api/v1/") }
            mappings shouldContain "/api/v1/teams"
            mappings shouldContain "/api/v1/teams/{teamId}/sub-services/{subService}"
        }
    })
