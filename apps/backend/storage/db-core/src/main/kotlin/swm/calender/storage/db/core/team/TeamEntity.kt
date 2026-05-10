package swm.calender.storage.db.core.team

import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.TeamMemberRole
import swm.calender.core.team.domain.model.SubServiceActivation
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.domain.model.TeamMember
import swm.calender.core.team.domain.model.TeamMemberHistory
import swm.calender.core.team.domain.model.TeamMemberHistoryAction
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

internal data class TeamEntity(
    val id: Long,
    val name: String,
    val description: String?,
    val inviteCode: String,
    val calendarEnabled: Boolean,
    val matchEnabled: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val calendarEnabledAt: LocalDateTime?,
    val matchEnabledAt: LocalDateTime?,
    val calendarDisabledAt: LocalDateTime?,
    val matchDisabledAt: LocalDateTime?,
) {
    fun toDomain(members: List<TeamMemberEntity>): Team {
        return Team(
            id = TeamId(id),
            name = name,
            description = description,
            inviteCode = inviteCode,
            members = members.map { it.toDomain() },
            subServiceActivation = SubServiceActivation(
                calendarEnabled = calendarEnabled,
                matchEnabled = matchEnabled,
                calendarEnabledAt = calendarEnabledAt?.toInstant(),
                matchEnabledAt = matchEnabledAt?.toInstant(),
                calendarDisabledAt = calendarDisabledAt?.toInstant(),
                matchDisabledAt = matchDisabledAt?.toInstant(),
            ),
            createdAt = createdAt.toInstant(),
            updatedAt = updatedAt.toInstant(),
        )
    }
}

internal data class TeamMemberEntity(
    val id: Long,
    val teamId: Long,
    val userId: Long,
    val name: String,
    val email: String,
    val role: TeamMemberRole,
    val joinedAt: LocalDateTime,
    val removedAt: LocalDateTime?,
) {
    fun toDomain(): TeamMember {
        return TeamMember(
            id = TeamMemberId(id),
            teamId = TeamId(teamId),
            userId = UserId(userId),
            name = name,
            email = email,
            role = role,
            joinedAt = joinedAt.toInstant(),
            removedAt = removedAt?.toInstant(),
        )
    }
}

internal data class TeamMemberHistoryEntity(
    val id: Long,
    val teamId: Long,
    val memberId: Long,
    val actorUserId: Long,
    val action: TeamMemberHistoryAction,
    val previousRole: TeamMemberRole,
    val changedRole: TeamMemberRole?,
    val occurredAt: LocalDateTime,
) {
    fun toDomain(): TeamMemberHistory {
        return TeamMemberHistory(
            id = id,
            teamId = TeamId(teamId),
            memberId = TeamMemberId(memberId),
            actorUserId = UserId(actorUserId),
            action = action,
            previousRole = previousRole,
            changedRole = changedRole,
            occurredAt = occurredAt.toInstant(),
        )
    }
}

internal fun Instant.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneOffset.UTC)

private fun LocalDateTime.toInstant(): Instant = toInstant(ZoneOffset.UTC)
