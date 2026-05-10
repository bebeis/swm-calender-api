package swm.calender.core.api.controller.v1.match.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.Platform

data class CandidateIdeaCreateRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String?,
    @field:NotBlank
    @field:Size(max = 300)
    val summary: String?,
    @field:NotBlank
    @field:Size(max = 1000)
    val problem: String?,
    @field:NotBlank
    @field:Size(max = 1000)
    val targetUsers: String?,
    @field:NotBlank
    @field:Size(max = 2000)
    val solution: String?,
    @field:NotNull
    val category: CampaignCategory?,
    @field:NotEmpty
    val platforms: List<Platform>?,
)
