package swm.calender.core.api.controller.v1.match.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.Platform
import java.time.OffsetDateTime

data class ServiceProfileCreateRequest(
    @field:NotBlank
    @field:Size(max = 80)
    val name: String?,
    @field:NotBlank
    @field:Size(max = 120)
    val summary: String?,
    @field:NotBlank
    @field:Size(max = 3000)
    val description: String?,
    @field:NotNull
    val category: CampaignCategory?,
    @field:NotEmpty
    val platforms: List<Platform>?,
    val screenshotUrls: List<String>? = emptyList(),
    val demoUrl: String? = null,
    @field:JsonProperty("public")
    @get:JsonProperty("public")
    val isPublic: Boolean? = true,
)

data class CampaignCreateRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String?,
    @field:NotBlank
    @field:Size(max = 3000)
    val description: String?,
    @field:NotNull
    @field:Min(1)
    val targetTeamCount: Int?,
    @field:NotNull
    @field:Future
    val deadline: OffsetDateTime?,
    @field:NotNull
    val reciprocalAvailable: Boolean?,
    @field:Size(max = 2000)
    val requirements: String? = null,
)

data class CampaignStatusChangeRequest(
    @field:NotNull
    val status: CampaignStatus?,
)
