package swm.calender.core.team.service.request

import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.SubService

data class TeamSubServiceActivationRequest(
    val teamId: TeamId,
    val actorUserId: UserId,
    val subService: SubService,
    val enabled: Boolean,
)
