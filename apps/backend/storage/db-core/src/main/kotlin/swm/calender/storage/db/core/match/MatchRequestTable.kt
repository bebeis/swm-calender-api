package swm.calender.storage.db.core.match

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import swm.calender.core.enums.AssignmentStatus
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.core.enums.MatchRequestType
import swm.calender.core.enums.NotificationType
import swm.calender.match.domain.model.NotificationReferenceType
import swm.calender.storage.db.core.team.TeamTable

object MatchRequestTable : Table("match_request") {
    val id = long("id").autoIncrement()
    val campaignId = long("campaign_id").references(BetaCampaignTable.id)
    val requestingTeamId = long("requesting_team_id").references(TeamTable.id)
    val targetTeamId = long("target_team_id").references(TeamTable.id)
    val requestType = enumerationByName<MatchRequestType>("request_type", 20)
    val requestStatus = enumerationByName<MatchRequestStatus>("request_status", 20)
    val message = varchar("message", 1000).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    init {
        index("ix_match_request_campaign_requesting_status", false, campaignId, requestingTeamId, requestStatus)
        index("ix_match_request_target_status_created_at", false, targetTeamId, requestStatus, createdAt)
    }

    override val primaryKey = PrimaryKey(id)
}

object MatchRequestStatusHistoryTable : Table("match_request_status_history") {
    val id = long("id").autoIncrement()
    val requestId = long("request_id").references(MatchRequestTable.id)
    val fromStatus = enumerationByName<MatchRequestStatus>("from_status", 20).nullable()
    val toStatus = enumerationByName<MatchRequestStatus>("to_status", 20)
    val changedByUserId = long("changed_by_user_id")
    val createdAt = datetime("created_at")

    init {
        index("ix_match_request_status_history_request_created_at", false, requestId, createdAt)
    }

    override val primaryKey = PrimaryKey(id)
}

object AssignmentTable : Table("match_assignment") {
    val id = long("id").autoIncrement()
    val requestId = long("request_id").references(MatchRequestTable.id).uniqueIndex()
    val testerTeamId = long("tester_team_id").references(TeamTable.id)
    val targetTeamId = long("target_team_id").references(TeamTable.id)
    val assignmentStatus = enumerationByName<AssignmentStatus>("assignment_status", 30)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    init {
        index("ix_assignment_tester_team_status_created_at", false, testerTeamId, assignmentStatus, createdAt)
        index("ix_assignment_target_team_status_created_at", false, targetTeamId, assignmentStatus, createdAt)
    }

    override val primaryKey = PrimaryKey(id)
}

object MatchNotificationTable : Table("match_notification") {
    val id = long("id").autoIncrement()
    val teamId = long("team_id").references(TeamTable.id)
    val notificationType = enumerationByName<NotificationType>("notification_type", 40)
    val referenceType = enumerationByName<NotificationReferenceType>("reference_type", 40)
    val referenceId = long("reference_id")
    val message = varchar("message", 500)
    val readAt = datetime("read_at").nullable()
    val createdAt = datetime("created_at")

    init {
        index("ix_match_notification_team_read_created_at", false, teamId, readAt, createdAt)
    }

    override val primaryKey = PrimaryKey(id)
}

internal fun ResultRow.toMatchRequestEntity(): MatchRequestEntity = MatchRequestEntity(
    id = this[MatchRequestTable.id],
    campaignId = this[MatchRequestTable.campaignId],
    requestingTeamId = this[MatchRequestTable.requestingTeamId],
    targetTeamId = this[MatchRequestTable.targetTeamId],
    type = this[MatchRequestTable.requestType],
    status = this[MatchRequestTable.requestStatus],
    message = this[MatchRequestTable.message],
    createdAt = this[MatchRequestTable.createdAt],
    updatedAt = this[MatchRequestTable.updatedAt],
)

internal fun ResultRow.toMatchRequestStatusHistoryEntity(): MatchRequestStatusHistoryEntity = MatchRequestStatusHistoryEntity(
    id = this[MatchRequestStatusHistoryTable.id],
    requestId = this[MatchRequestStatusHistoryTable.requestId],
    fromStatus = this[MatchRequestStatusHistoryTable.fromStatus],
    toStatus = this[MatchRequestStatusHistoryTable.toStatus],
    changedByUserId = this[MatchRequestStatusHistoryTable.changedByUserId],
    createdAt = this[MatchRequestStatusHistoryTable.createdAt],
)

internal fun ResultRow.toAssignmentEntity(): AssignmentEntity = AssignmentEntity(
    id = this[AssignmentTable.id],
    requestId = this[AssignmentTable.requestId],
    testerTeamId = this[AssignmentTable.testerTeamId],
    targetTeamId = this[AssignmentTable.targetTeamId],
    status = this[AssignmentTable.assignmentStatus],
    createdAt = this[AssignmentTable.createdAt],
    updatedAt = this[AssignmentTable.updatedAt],
)

internal fun ResultRow.toNotificationEntity(): NotificationEntity = NotificationEntity(
    id = this[MatchNotificationTable.id],
    teamId = this[MatchNotificationTable.teamId],
    type = this[MatchNotificationTable.notificationType],
    referenceType = this[MatchNotificationTable.referenceType],
    referenceId = this[MatchNotificationTable.referenceId],
    message = this[MatchNotificationTable.message],
    readAt = this[MatchNotificationTable.readAt],
    createdAt = this[MatchNotificationTable.createdAt],
)
