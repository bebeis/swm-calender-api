package swm.calender.core.api.security

import org.springframework.stereotype.Component
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.TeamMemberRole

@Component
class TeamAuthorization {
    fun requireMember(
        user: AuthenticatedUser,
        teamId: TeamId,
        members: Collection<TeamMembershipAuthority>,
    ): TeamMembershipAuthority {
        val membership = members.firstOrNull { it.teamId == teamId && it.userId == user.userId }
            ?: throw TeamApiAuthorizationException.forbidden("The authenticated user is not a member of this team.")
        return membership
    }

    fun requireOwner(
        user: AuthenticatedUser,
        teamId: TeamId,
        members: Collection<TeamMembershipAuthority>,
    ): TeamMembershipAuthority {
        val membership = requireMember(user, teamId, members)
        if (membership.role != TeamMemberRole.OWNER) {
            throw TeamApiAuthorizationException.forbidden("Only team owners can perform this action.")
        }
        return membership
    }
}

data class TeamMembershipAuthority(
    val memberId: TeamMemberId,
    val teamId: TeamId,
    val userId: UserId,
    val role: TeamMemberRole,
)

class TeamApiAuthorizationException(
    val reason: String,
    val statusCode: Int,
) : RuntimeException(reason) {
    companion object {
        fun forbidden(reason: String): TeamApiAuthorizationException {
            return TeamApiAuthorizationException(reason, 403)
        }
    }
}
