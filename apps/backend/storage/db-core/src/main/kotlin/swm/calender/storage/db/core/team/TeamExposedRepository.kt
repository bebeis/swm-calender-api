package swm.calender.storage.db.core.team

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.team.domain.TeamRepository
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.domain.model.TeamMember
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage

@Repository
class TeamExposedRepository : TeamRepository {
    override fun save(team: Team): Team {
        return if (team.id == null) {
            create(team)
        } else {
            update(team)
        }
    }

    override fun findById(teamId: TeamId): Team? {
        return findByIdInternal(teamId)
    }

    override fun findByInviteCode(inviteCode: String): Team? {
        val teamId = teamJoinQuery()
            .selectAll()
            .where { TeamTable.inviteCode eq inviteCode }
            .singleOrNull()
            ?.get(TeamTable.id)

        return teamId?.let { findByIdInternal(TeamId(it)) }
    }

    override fun findActiveByUserId(userId: UserId): Team? {
        val teamId = TeamMemberTable
            .selectAll()
            .where {
                (TeamMemberTable.userId eq userId.value) and
                    (TeamMemberTable.removedAt eq null)
            }
            .singleOrNull()
            ?.get(TeamMemberTable.teamId)

        return teamId?.let { findByIdInternal(TeamId(it)) }
    }

    override fun existsActiveMembershipByUserId(userId: UserId): Boolean {
        return activeMembershipExists(userId)
    }

    private fun create(team: Team): Team {
        team.members
            .filter { it.isActive() }
            .forEach { validateActiveMembershipDoesNotExist(it.userId) }

        val savedTeamId = TeamTable.insert {
            it[name] = team.name
            it[description] = team.description
            it[inviteCode] = team.inviteCode
            it[createdAt] = team.createdAt.toLocalDateTime()
            it[updatedAt] = team.updatedAt.toLocalDateTime()
        }[TeamTable.id]
        val teamId = TeamId(savedTeamId)

        team.members.forEach { insertMember(teamId, it) }
        insertSubServiceActivation(teamId, team)

        return requireNotNull(findByIdInternal(teamId))
    }

    private fun update(team: Team): Team {
        val teamId = team.requireId()

        TeamTable.update(
            where = { TeamTable.id eq teamId.value },
        ) {
            it[name] = team.name
            it[description] = team.description
            it[updatedAt] = team.updatedAt.toLocalDateTime()
        }

        updateSubServiceActivation(teamId, team)
        team.members.forEach { member ->
            if (member.id == null) {
                validateActiveMembershipDoesNotExist(member.userId)
                insertMember(teamId, member)
            } else {
                updateMember(member)
            }
        }

        return requireNotNull(findByIdInternal(teamId))
    }

    private fun insertMember(
        teamId: TeamId,
        member: TeamMember,
    ) {
        TeamMemberTable.insert {
            it[TeamMemberTable.teamId] = teamId.value
            it[userId] = member.userId.value
            it[memberName] = member.name
            it[memberEmail] = member.email
            it[role] = member.role
            it[joinedAt] = member.joinedAt.toLocalDateTime()
            it[removedAt] = member.removedAt?.toLocalDateTime()
        }
    }

    private fun updateMember(member: TeamMember) {
        val memberId = requireNotNull(member.id)
        TeamMemberTable.update(
            where = { TeamMemberTable.id eq memberId.value },
        ) {
            it[memberName] = member.name
            it[memberEmail] = member.email
            it[role] = member.role
            it[removedAt] = member.removedAt?.toLocalDateTime()
        }
    }

    private fun insertSubServiceActivation(
        teamId: TeamId,
        team: Team,
    ) {
        SubServiceActivationTable.insert {
            it[SubServiceActivationTable.teamId] = teamId.value
            it[calendarEnabled] = team.subServiceActivation.calendarEnabled
            it[matchEnabled] = team.subServiceActivation.matchEnabled
            it[calendarEnabledAt] = team.subServiceActivation.calendarEnabledAt?.toLocalDateTime()
            it[matchEnabledAt] = team.subServiceActivation.matchEnabledAt?.toLocalDateTime()
            it[calendarDisabledAt] = team.subServiceActivation.calendarDisabledAt?.toLocalDateTime()
            it[matchDisabledAt] = team.subServiceActivation.matchDisabledAt?.toLocalDateTime()
        }
    }

    private fun updateSubServiceActivation(
        teamId: TeamId,
        team: Team,
    ) {
        SubServiceActivationTable.update(
            where = { SubServiceActivationTable.teamId eq teamId.value },
        ) {
            it[calendarEnabled] = team.subServiceActivation.calendarEnabled
            it[matchEnabled] = team.subServiceActivation.matchEnabled
            it[calendarEnabledAt] = team.subServiceActivation.calendarEnabledAt?.toLocalDateTime()
            it[matchEnabledAt] = team.subServiceActivation.matchEnabledAt?.toLocalDateTime()
            it[calendarDisabledAt] = team.subServiceActivation.calendarDisabledAt?.toLocalDateTime()
            it[matchDisabledAt] = team.subServiceActivation.matchDisabledAt?.toLocalDateTime()
        }
    }

    private fun findByIdInternal(teamId: TeamId): Team? {
        val teamEntity = teamJoinQuery()
            .selectAll()
            .where { TeamTable.id eq teamId.value }
            .singleOrNull()
            ?.toTeamEntity()
            ?: return null

        val memberEntities = TeamMemberTable
            .selectAll()
            .where { TeamMemberTable.teamId eq teamId.value }
            .orderBy(TeamMemberTable.id)
            .map { it.toTeamMemberEntity() }

        return teamEntity.toDomain(memberEntities)
    }

    private fun validateActiveMembershipDoesNotExist(userId: UserId) {
        if (activeMembershipExists(userId)) {
            throw TeamDomainException(TeamErrorMessage.TEAM_ALREADY_EXISTS_FOR_USER)
        }
    }

    private fun activeMembershipExists(userId: UserId): Boolean {
        return TeamMemberTable
            .selectAll()
            .where {
                (TeamMemberTable.userId eq userId.value) and
                    (TeamMemberTable.removedAt eq null)
            }
            .limit(1)
            .any()
    }

    private fun teamJoinQuery() = TeamTable.join(
        otherTable = SubServiceActivationTable,
        joinType = JoinType.INNER,
        onColumn = TeamTable.id,
        otherColumn = SubServiceActivationTable.teamId,
    )
}
