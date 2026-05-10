package swm.calender.core.team.service.request

import swm.calender.core.common.id.UserId

data class TeamCreateRequest(
    val ownerUserId: UserId,
    val ownerName: String,
    val ownerEmail: String,
    val name: String,
    val description: String? = null,
)
