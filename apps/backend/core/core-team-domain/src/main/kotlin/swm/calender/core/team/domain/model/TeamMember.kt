package swm.calender.core.team.domain.model

import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.TeamMemberRole
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import java.time.Instant

data class TeamMember(
    val id: TeamMemberId? = null,
    val teamId: TeamId? = null,
    val userId: UserId,
    val name: String,
    val email: String,
    val role: TeamMemberRole,
    val joinedAt: Instant,
    val removedAt: Instant? = null,
) {
    init {
        validateProfile(name = name, email = email)
    }

    fun isActive(): Boolean = removedAt == null

    fun isOwner(): Boolean = role == TeamMemberRole.OWNER

    fun isActiveOwner(): Boolean = isActive() && isOwner()

    fun belongsTo(userId: UserId): Boolean = this.userId == userId

    fun changeRole(role: TeamMemberRole): TeamMember {
        if (!isActive()) {
            throw TeamDomainException(TeamErrorMessage.TEAM_MEMBER_INACTIVE)
        }
        return copy(role = role)
    }

    fun remove(removedAt: Instant): TeamMember {
        if (!isActive()) {
            throw TeamDomainException(TeamErrorMessage.TEAM_MEMBER_INACTIVE)
        }
        return copy(removedAt = removedAt)
    }

    companion object {
        fun createOwner(
            userId: UserId,
            name: String,
            email: String,
            joinedAt: Instant,
            teamId: TeamId? = null,
        ): TeamMember {
            return TeamMember(
                userId = userId,
                name = name.trim(),
                email = email.trim(),
                role = TeamMemberRole.OWNER,
                joinedAt = joinedAt,
                teamId = teamId,
            )
        }

        fun createMember(
            userId: UserId,
            name: String,
            email: String,
            joinedAt: Instant,
            teamId: TeamId? = null,
        ): TeamMember {
            return TeamMember(
                userId = userId,
                name = name.trim(),
                email = email.trim(),
                role = TeamMemberRole.MEMBER,
                joinedAt = joinedAt,
                teamId = teamId,
            )
        }

        private fun validateProfile(
            name: String,
            email: String,
        ) {
            if (name.isBlank()) {
                throw TeamDomainException(TeamErrorMessage.TEAM_MEMBER_NAME_REQUIRED)
            }
            if (email.isBlank()) {
                throw TeamDomainException(TeamErrorMessage.TEAM_MEMBER_EMAIL_REQUIRED)
            }
        }
    }
}
