package swm.calender.core.team.service.request

import swm.calender.core.common.id.UserId

data class TeamJoinRequest(
    val userId: UserId,
    val name: String,
    val email: String,
    val inviteCode: String,
)
