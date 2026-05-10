package swm.calender.core.api.controller.v1.team.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TeamJoinRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val inviteCode: String,
)
