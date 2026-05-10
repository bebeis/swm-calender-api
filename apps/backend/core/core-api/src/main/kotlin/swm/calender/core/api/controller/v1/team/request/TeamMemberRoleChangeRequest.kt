package swm.calender.core.api.controller.v1.team.request

import swm.calender.core.enums.TeamMemberRole

data class TeamMemberRoleChangeRequest(
    val role: TeamMemberRole?,
)
