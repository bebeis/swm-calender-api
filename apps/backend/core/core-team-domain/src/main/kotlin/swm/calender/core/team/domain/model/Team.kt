package swm.calender.core.team.domain.model

import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.SubService
import swm.calender.core.enums.TeamMemberRole
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import java.time.Instant

data class Team(
    val id: TeamId? = null,
    val name: String,
    val description: String? = null,
    val inviteCode: String,
    val members: List<TeamMember>,
    val subServiceActivation: SubServiceActivation,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        validateName(name)
        validateInviteCode(inviteCode)
        validateOwners(members)
    }

    fun addMember(
        userId: UserId,
        name: String,
        email: String,
        joinedAt: Instant,
    ): Team {
        if (members.any { it.belongsTo(userId) && it.isActive() }) {
            throw TeamDomainException(TeamErrorMessage.TEAM_MEMBER_ALREADY_EXISTS)
        }

        return copy(
            members = members + TeamMember.createMember(
                userId = userId,
                name = name,
                email = email,
                joinedAt = joinedAt,
                teamId = id,
            ),
            updatedAt = joinedAt,
        )
    }

    fun changeSubServiceActivation(
        subService: SubService,
        enabled: Boolean,
        actorUserId: UserId,
        occurredAt: Instant,
    ): Team {
        requireOwner(actorUserId)

        return copy(
            subServiceActivation = subServiceActivation.change(
                subService = subService,
                enabled = enabled,
                occurredAt = occurredAt,
            ),
            updatedAt = occurredAt,
        )
    }

    fun changeMemberRole(
        memberId: TeamMemberId,
        role: TeamMemberRole,
        actorUserId: UserId,
        occurredAt: Instant,
    ): Team {
        requireOwner(actorUserId)

        var targetFound = false
        val changedMembers = members.map { member ->
            if (member.id == memberId) {
                targetFound = true
                member.changeRole(role)
            } else {
                member
            }
        }
        if (!targetFound) {
            throw TeamDomainException(TeamErrorMessage.TEAM_MEMBER_NOT_FOUND)
        }
        validateOwners(changedMembers)

        return copy(
            members = changedMembers,
            updatedAt = occurredAt,
        )
    }

    fun removeMember(
        memberId: TeamMemberId,
        actorUserId: UserId,
        occurredAt: Instant,
    ): Team {
        requireOwner(actorUserId)

        var targetFound = false
        val changedMembers = members.map { member ->
            if (member.id == memberId) {
                targetFound = true
                member.remove(occurredAt)
            } else {
                member
            }
        }
        if (!targetFound) {
            throw TeamDomainException(TeamErrorMessage.TEAM_MEMBER_NOT_FOUND)
        }
        validateOwners(changedMembers)

        return copy(
            members = changedMembers,
            updatedAt = occurredAt,
        )
    }

    fun getMember(memberId: TeamMemberId): TeamMember {
        return members.singleOrNull { it.id == memberId }
            ?: throw TeamDomainException(TeamErrorMessage.TEAM_MEMBER_NOT_FOUND)
    }

    fun requireMember(userId: UserId) {
        if (members.none { it.belongsTo(userId) && it.isActive() }) {
            throw TeamDomainException(TeamErrorMessage.TEAM_MEMBER_REQUIRED)
        }
    }

    fun requireId(): TeamId {
        return id ?: throw TeamDomainException(TeamErrorMessage.TEAM_NOT_PERSISTED)
    }

    fun isOwner(userId: UserId): Boolean {
        return members.any { it.belongsTo(userId) && it.isActiveOwner() }
    }

    private fun requireOwner(userId: UserId) {
        if (!isOwner(userId)) {
            throw TeamDomainException(TeamErrorMessage.TEAM_OWNER_REQUIRED)
        }
    }

    companion object {
        fun create(
            name: String,
            description: String?,
            inviteCode: String,
            ownerUserId: UserId,
            ownerName: String,
            ownerEmail: String,
            createdAt: Instant,
        ): Team {
            return Team(
                name = name.trim(),
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                inviteCode = inviteCode.trim(),
                members = listOf(
                    TeamMember.createOwner(
                        userId = ownerUserId,
                        name = ownerName,
                        email = ownerEmail,
                        joinedAt = createdAt,
                    ),
                ),
                subServiceActivation = SubServiceActivation.inactive(),
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }

        private fun validateName(name: String) {
            if (name.isBlank()) {
                throw TeamDomainException(TeamErrorMessage.TEAM_NAME_REQUIRED)
            }
        }

        private fun validateInviteCode(inviteCode: String) {
            if (inviteCode.isBlank()) {
                throw TeamDomainException(TeamErrorMessage.INVITE_CODE_REQUIRED)
            }
        }

        private fun validateOwners(members: List<TeamMember>) {
            if (members.none { it.isActiveOwner() }) {
                throw TeamDomainException(TeamErrorMessage.TEAM_ACTIVE_OWNER_REQUIRED)
            }
        }
    }
}
