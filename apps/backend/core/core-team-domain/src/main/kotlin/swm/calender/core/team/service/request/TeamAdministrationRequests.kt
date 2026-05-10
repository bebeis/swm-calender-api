package swm.calender.core.team.service.request

import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.TeamMemberRole

data class TeamMemberRoleChangeRequest(
    val teamId: TeamId,
    val memberId: TeamMemberId,
    val actorUserId: UserId,
    val role: TeamMemberRole,
)

data class TeamMemberRemovalRequest(
    val teamId: TeamId,
    val memberId: TeamMemberId,
    val actorUserId: UserId,
)
