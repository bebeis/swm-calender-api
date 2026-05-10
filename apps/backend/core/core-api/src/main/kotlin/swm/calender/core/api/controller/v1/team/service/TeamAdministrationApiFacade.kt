package swm.calender.core.api.controller.v1.team.service

import org.springframework.stereotype.Component
import swm.calender.core.api.controller.v1.team.request.TeamMemberRoleChangeRequest
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.team.service.TeamAdministrationService
import swm.calender.core.team.service.request.TeamMemberRemovalRequest
import swm.calender.core.team.service.request.TeamMemberRoleChangeRequest as TeamServiceMemberRoleChangeRequest

@Component
class TeamAdministrationApiFacade(
    private val teamAdministrationService: TeamAdministrationService,
) {
    fun changeMemberRole(
        user: AuthenticatedUser,
        teamId: TeamId,
        memberId: TeamMemberId,
        request: TeamMemberRoleChangeRequest,
    ): TeamMemberSnapshot {
        return TeamMemberSnapshot.from(
            teamAdministrationService.changeMemberRole(
                TeamServiceMemberRoleChangeRequest(
                    teamId = teamId,
                    memberId = memberId,
                    actorUserId = user.userId,
                    role = requireNotNull(request.role),
                ),
            ),
        )
    }

    fun removeMember(
        user: AuthenticatedUser,
        teamId: TeamId,
        memberId: TeamMemberId,
    ): TeamMemberSnapshot {
        return TeamMemberSnapshot.from(
            teamAdministrationService.removeMember(
                TeamMemberRemovalRequest(
                    teamId = teamId,
                    memberId = memberId,
                    actorUserId = user.userId,
                ),
            ),
        )
    }
}
