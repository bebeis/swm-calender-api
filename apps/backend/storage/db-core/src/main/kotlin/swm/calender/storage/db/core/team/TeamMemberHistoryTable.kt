package swm.calender.storage.db.core.team

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import swm.calender.core.enums.TeamMemberRole
import swm.calender.core.team.domain.model.TeamMemberHistoryAction

object TeamMemberHistoryTable : Table("team_member_history") {
    val id = long("id").autoIncrement()
    val teamId = long("team_id").references(TeamTable.id)
    val memberId = long("member_id").references(TeamMemberTable.id)
    val actorUserId = long("actor_user_id")
    val action = enumerationByName<TeamMemberHistoryAction>("history_action", 30)
    val previousRole = enumerationByName<TeamMemberRole>("previous_role", 20)
    val changedRole = enumerationByName<TeamMemberRole>("changed_role", 20).nullable()
    val occurredAt = datetime("occurred_at")

    init {
        index("ix_team_member_history_team_id_occurred_at", false, teamId, occurredAt)
        index("ix_team_member_history_member_id_occurred_at", false, memberId, occurredAt)
    }

    override val primaryKey = PrimaryKey(id)
}

internal fun ResultRow.toTeamMemberHistoryEntity(): TeamMemberHistoryEntity = TeamMemberHistoryEntity(
    id = this[TeamMemberHistoryTable.id],
    teamId = this[TeamMemberHistoryTable.teamId],
    memberId = this[TeamMemberHistoryTable.memberId],
    actorUserId = this[TeamMemberHistoryTable.actorUserId],
    action = this[TeamMemberHistoryTable.action],
    previousRole = this[TeamMemberHistoryTable.previousRole],
    changedRole = this[TeamMemberHistoryTable.changedRole],
    occurredAt = this[TeamMemberHistoryTable.occurredAt],
)
