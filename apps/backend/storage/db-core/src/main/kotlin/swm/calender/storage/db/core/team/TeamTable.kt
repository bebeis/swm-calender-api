package swm.calender.storage.db.core.team

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import swm.calender.core.enums.TeamMemberRole

object TeamTable : Table("team") {
    val id = long("id").autoIncrement()
    val name = varchar("team_name", 50)
    val description = varchar("description", 500).nullable()
    val inviteCode = varchar("invite_code", 64).uniqueIndex()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object TeamMemberTable : Table("team_member") {
    val id = long("id").autoIncrement()
    val teamId = long("team_id").references(TeamTable.id)
    val userId = long("user_id")
    val memberName = varchar("member_name", 100)
    val memberEmail = varchar("member_email", 255)
    val role = enumerationByName<TeamMemberRole>("member_role", 20)
    val joinedAt = datetime("joined_at")
    val removedAt = datetime("removed_at").nullable()

    init {
        uniqueIndex("ux_team_member_team_id_user_id", teamId, userId)
        index("ix_team_member_team_id_removed_at", false, teamId, removedAt)
        index("ix_team_member_user_id_removed_at", false, userId, removedAt)
    }

    override val primaryKey = PrimaryKey(id)
}

object SubServiceActivationTable : Table("sub_service_activation") {
    val teamId = long("team_id").references(TeamTable.id).uniqueIndex()
    val calendarEnabled = bool("calendar_enabled")
    val matchEnabled = bool("match_enabled")
    val calendarEnabledAt = datetime("calendar_enabled_at").nullable()
    val matchEnabledAt = datetime("match_enabled_at").nullable()
    val calendarDisabledAt = datetime("calendar_disabled_at").nullable()
    val matchDisabledAt = datetime("match_disabled_at").nullable()

    override val primaryKey = PrimaryKey(teamId)
}

internal fun ResultRow.toTeamEntity(): TeamEntity = TeamEntity(
    id = this[TeamTable.id],
    name = this[TeamTable.name],
    description = this[TeamTable.description],
    inviteCode = this[TeamTable.inviteCode],
    calendarEnabled = this[SubServiceActivationTable.calendarEnabled],
    matchEnabled = this[SubServiceActivationTable.matchEnabled],
    createdAt = this[TeamTable.createdAt],
    updatedAt = this[TeamTable.updatedAt],
    calendarEnabledAt = this[SubServiceActivationTable.calendarEnabledAt],
    matchEnabledAt = this[SubServiceActivationTable.matchEnabledAt],
    calendarDisabledAt = this[SubServiceActivationTable.calendarDisabledAt],
    matchDisabledAt = this[SubServiceActivationTable.matchDisabledAt],
)

internal fun ResultRow.toTeamMemberEntity(): TeamMemberEntity = TeamMemberEntity(
    id = this[TeamMemberTable.id],
    teamId = this[TeamMemberTable.teamId],
    userId = this[TeamMemberTable.userId],
    name = this[TeamMemberTable.memberName],
    email = this[TeamMemberTable.memberEmail],
    role = this[TeamMemberTable.role],
    joinedAt = this[TeamMemberTable.joinedAt],
    removedAt = this[TeamMemberTable.removedAt],
)
