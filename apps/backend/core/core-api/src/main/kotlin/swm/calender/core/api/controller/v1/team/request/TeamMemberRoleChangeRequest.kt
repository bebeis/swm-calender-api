package swm.calender.core.api.controller.v1.team.request

import jakarta.validation.constraints.NotNull
import swm.calender.core.enums.TeamMemberRole

data class TeamMemberRoleChangeRequest(
    @field:NotNull
    val role: TeamMemberRole?,
)
