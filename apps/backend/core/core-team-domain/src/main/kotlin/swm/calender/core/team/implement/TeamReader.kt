package swm.calender.core.team.implement

import org.springframework.stereotype.Component
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.team.domain.TeamRepository
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage

@Component
class TeamReader(
    private val teamRepository: TeamRepository,
) {
    fun ensureUserHasNoActiveTeam(userId: UserId) {
        if (teamRepository.existsActiveMembershipByUserId(userId)) {
            throw TeamDomainException(TeamErrorMessage.TEAM_ALREADY_EXISTS_FOR_USER)
        }
    }

    fun getById(teamId: TeamId): Team {
        return teamRepository.findById(teamId)
            ?: throw TeamDomainException(TeamErrorMessage.TEAM_NOT_FOUND)
    }

    fun getByInviteCode(inviteCode: String): Team {
        return teamRepository.findByInviteCode(inviteCode)
            ?: throw TeamDomainException(TeamErrorMessage.INVALID_INVITE_CODE)
    }
}
