package swm.calender.core.api.controller.v1.team.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TeamCreateRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val name: String,
    @field:Size(max = 500)
    val description: String? = null,
)
