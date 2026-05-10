package swm.calender.core.api.controller.v1.team.service

import org.springframework.stereotype.Component
import swm.calender.core.api.controller.v1.team.request.TeamCreateRequest
import swm.calender.core.api.controller.v1.team.request.TeamJoinRequest
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.enums.SubService
import swm.calender.core.enums.TeamMemberRole
import swm.calender.core.support.error.ErrorType
import swm.calender.core.team.service.TeamService
import swm.calender.core.team.service.request.TeamSubServiceActivationRequest
import swm.calender.core.team.service.request.TeamCreateRequest as TeamServiceCreateRequest
import swm.calender.core.team.service.request.TeamJoinRequest as TeamServiceJoinRequest
import swm.calender.core.team.service.response.SubServiceActivationResponse as TeamServiceSubServiceActivationResponse
import swm.calender.core.team.service.response.TeamMemberResponse as TeamServiceMemberResponse
import swm.calender.core.team.service.response.TeamResponse as TeamServiceResponse

@Component
class TeamApiFacade(
    private val teamService: TeamService,
) {
    fun createTeam(
        user: AuthenticatedUser,
        request: TeamCreateRequest,
    ): TeamSnapshot {
        return TeamSnapshot.from(
            teamService.createTeam(
                TeamServiceCreateRequest(
                    ownerUserId = user.userId,
                    ownerName = user.name,
                    ownerEmail = user.email,
                    name = request.name,
                    description = request.description,
                ),
            ),
        )
    }

    fun joinTeam(
        user: AuthenticatedUser,
        request: TeamJoinRequest,
    ): TeamSnapshot {
        return TeamSnapshot.from(
            teamService.joinTeam(
                TeamServiceJoinRequest(
                    userId = user.userId,
                    name = user.name,
                    email = user.email,
                    inviteCode = request.inviteCode,
                ),
            ),
        )
    }

    fun listMembers(
        user: AuthenticatedUser,
        teamId: TeamId,
    ): List<TeamMemberSnapshot> {
        return teamService.getMembers(
            teamId = teamId,
            actorUserId = user.userId,
        ).map(TeamMemberSnapshot::from)
    }

    fun changeMemberRole(
        user: AuthenticatedUser,
        teamId: TeamId,
        memberId: TeamMemberId,
        role: TeamMemberRole,
    ): TeamMemberSnapshot {
        return TeamMemberSnapshot.from(
            teamService.changeMemberRole(
                teamId = teamId,
                memberId = memberId,
                role = role,
                actorUserId = user.userId,
            ),
        )
    }

    fun updateSubServiceActivation(
        user: AuthenticatedUser,
        teamId: TeamId,
        subService: SubService,
        enabled: Boolean,
    ): SubServiceActivationSnapshot {
        return SubServiceActivationSnapshot.from(
            teamService.changeSubServiceActivation(
                TeamSubServiceActivationRequest(
                    teamId = teamId,
                    actorUserId = user.userId,
                    subService = subService,
                    enabled = enabled,
                ),
            ),
        )
    }
}

data class TeamSnapshot(
    val teamId: Long,
    val name: String,
    val description: String?,
    val inviteCode: String,
    val calendarEnabled: Boolean,
    val matchEnabled: Boolean,
) {
    companion object {
        fun from(response: TeamServiceResponse): TeamSnapshot {
            return TeamSnapshot(
                teamId = response.teamId,
                name = response.name,
                description = response.description,
                inviteCode = response.inviteCode,
                calendarEnabled = response.calendarEnabled,
                matchEnabled = response.matchEnabled,
            )
        }
    }
}

data class TeamMemberSnapshot(
    val memberId: Long,
    val userId: Long,
    val name: String,
    val email: String,
    val role: TeamMemberRole,
) {
    companion object {
        fun from(response: TeamServiceMemberResponse): TeamMemberSnapshot {
            return TeamMemberSnapshot(
                memberId = response.memberId,
                userId = response.userId,
                name = response.name,
                email = response.email,
                role = response.role,
            )
        }
    }
}

data class SubServiceActivationSnapshot(
    val teamId: Long,
    val calendarEnabled: Boolean,
    val matchEnabled: Boolean,
) {
    companion object {
        fun from(response: TeamServiceSubServiceActivationResponse): SubServiceActivationSnapshot {
            return SubServiceActivationSnapshot(
                teamId = response.teamId,
                calendarEnabled = response.calendarEnabled,
                matchEnabled = response.matchEnabled,
            )
        }
    }
}

class TeamApiException(
    val errorType: ErrorType,
    override val message: String,
) : RuntimeException(message) {
    companion object {
        fun badRequest(message: String): TeamApiException = TeamApiException(ErrorType.VALIDATION_ERROR, message)

        fun notFound(message: String): TeamApiException = TeamApiException(ErrorType.RESOURCE_NOT_FOUND, message)

        fun conflict(message: String): TeamApiException = TeamApiException(ErrorType.DUPLICATE_RESOURCE, message)
    }
}
