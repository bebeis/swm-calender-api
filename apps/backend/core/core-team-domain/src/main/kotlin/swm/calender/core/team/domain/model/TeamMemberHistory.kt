package swm.calender.core.team.domain.model

import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.TeamMemberRole
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import java.time.Instant

data class TeamMemberHistory(
    val id: Long? = null,
    val teamId: TeamId,
    val memberId: TeamMemberId,
    val actorUserId: UserId,
    val action: TeamMemberHistoryAction,
    val previousRole: TeamMemberRole,
    val changedRole: TeamMemberRole?,
    val occurredAt: Instant,
) {
    init {
        validateRoleFields(action = action, changedRole = changedRole)
    }

    companion object {
        fun roleChanged(
            teamId: TeamId,
            memberId: TeamMemberId,
            actorUserId: UserId,
            previousRole: TeamMemberRole,
            changedRole: TeamMemberRole,
            occurredAt: Instant,
        ): TeamMemberHistory {
            return TeamMemberHistory(
                teamId = teamId,
                memberId = memberId,
                actorUserId = actorUserId,
                action = TeamMemberHistoryAction.ROLE_CHANGED,
                previousRole = previousRole,
                changedRole = changedRole,
                occurredAt = occurredAt,
            )
        }

        fun memberRemoved(
            teamId: TeamId,
            memberId: TeamMemberId,
            actorUserId: UserId,
            previousRole: TeamMemberRole,
            occurredAt: Instant,
        ): TeamMemberHistory {
            return TeamMemberHistory(
                teamId = teamId,
                memberId = memberId,
                actorUserId = actorUserId,
                action = TeamMemberHistoryAction.MEMBER_REMOVED,
                previousRole = previousRole,
                changedRole = null,
                occurredAt = occurredAt,
            )
        }

        private fun validateRoleFields(
            action: TeamMemberHistoryAction,
            changedRole: TeamMemberRole?,
        ) {
            if (action == TeamMemberHistoryAction.ROLE_CHANGED && changedRole == null) {
                throw TeamDomainException(TeamErrorMessage.TEAM_MEMBER_HISTORY_ROLE_REQUIRED)
            }
            if (action == TeamMemberHistoryAction.MEMBER_REMOVED && changedRole != null) {
                throw TeamDomainException(TeamErrorMessage.TEAM_MEMBER_HISTORY_ROLE_REQUIRED)
            }
        }
    }
}

enum class TeamMemberHistoryAction {
    ROLE_CHANGED,
    MEMBER_REMOVED,
}
