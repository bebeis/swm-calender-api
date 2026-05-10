package swm.calender.storage.db.core.match

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.match.domain.MatchRequestRepository
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.MatchRequest
import swm.calender.match.domain.model.MatchRequestStatusHistory
import swm.calender.match.domain.model.Notification

@Repository
class MatchRequestExposedRepository : MatchRequestRepository {
    override fun saveRequest(matchRequest: MatchRequest): MatchRequest {
        val savedRequestId = matchRequest.id?.value?.takeIf { requestRowExists(it) }
            ?.also { updateRequest(it, matchRequest) }
            ?: insertRequest(matchRequest)

        return requireNotNull(findRequestById(RequestId(savedRequestId)))
    }

    override fun findRequestById(requestId: RequestId): MatchRequest? {
        return MatchRequestTable
            .selectAll()
            .where { MatchRequestTable.id eq requestId.value }
            .singleOrNull()
            ?.toMatchRequestEntity()
            ?.toDomain()
    }

    override fun existsActiveRequestByCampaignIdAndRequestingTeamId(
        campaignId: CampaignId,
        requestingTeamId: TeamId,
    ): Boolean {
        return MatchRequestTable
            .selectAll()
            .where {
                (MatchRequestTable.campaignId eq campaignId.value) and
                    (MatchRequestTable.requestingTeamId eq requestingTeamId.value) and
                    (MatchRequestTable.requestStatus inList ACTIVE_REQUEST_STATUSES)
            }
            .limit(1)
            .any()
    }

    override fun saveStatusHistory(history: MatchRequestStatusHistory): MatchRequestStatusHistory {
        val historyId = MatchRequestStatusHistoryTable.insert {
            it[requestId] = history.requestId.value
            it[fromStatus] = history.fromStatus
            it[toStatus] = history.toStatus
            it[changedByUserId] = history.changedByUserId.value
            it[createdAt] = history.createdAt.toLocalDateTime()
        }[MatchRequestStatusHistoryTable.id]

        return history.copy(id = historyId)
    }

    override fun saveAssignment(assignment: Assignment): Assignment {
        val savedAssignmentId = assignment.id?.value?.takeIf { assignmentRowExists(it) }
            ?.also { updateAssignment(it, assignment) }
            ?: insertAssignment(assignment)

        return requireNotNull(findAssignmentById(AssignmentId(savedAssignmentId)))
    }

    override fun findAssignmentByRequestId(requestId: RequestId): Assignment? {
        return AssignmentTable
            .selectAll()
            .where { AssignmentTable.requestId eq requestId.value }
            .singleOrNull()
            ?.toAssignmentEntity()
            ?.toDomain()
    }

    override fun findAssignmentById(assignmentId: AssignmentId): Assignment? {
        return AssignmentTable
            .selectAll()
            .where { AssignmentTable.id eq assignmentId.value }
            .singleOrNull()
            ?.toAssignmentEntity()
            ?.toDomain()
    }

    override fun saveNotification(notification: Notification): Notification {
        val notificationId = MatchNotificationTable.insert {
            it[teamId] = notification.teamId.value
            it[notificationType] = notification.type
            it[referenceType] = notification.referenceType
            it[referenceId] = notification.referenceId
            it[message] = notification.message
            it[readAt] = notification.readAt?.toLocalDateTime()
            it[createdAt] = notification.createdAt.toLocalDateTime()
        }[MatchNotificationTable.id]

        return notification.copy(id = notificationId)
    }

    private fun insertRequest(matchRequest: MatchRequest): Long {
        return MatchRequestTable.insert {
            it[campaignId] = matchRequest.campaignId.value
            it[requestingTeamId] = matchRequest.requestingTeamId.value
            it[targetTeamId] = matchRequest.targetTeamId.value
            it[requestType] = matchRequest.type
            it[requestStatus] = matchRequest.status
            it[message] = matchRequest.message
            it[createdAt] = matchRequest.createdAt.toLocalDateTime()
            it[updatedAt] = matchRequest.updatedAt.toLocalDateTime()
        }[MatchRequestTable.id]
    }

    private fun updateRequest(
        requestId: Long,
        matchRequest: MatchRequest,
    ) {
        MatchRequestTable.update(
            where = { MatchRequestTable.id eq requestId },
        ) {
            it[campaignId] = matchRequest.campaignId.value
            it[requestingTeamId] = matchRequest.requestingTeamId.value
            it[targetTeamId] = matchRequest.targetTeamId.value
            it[requestType] = matchRequest.type
            it[requestStatus] = matchRequest.status
            it[message] = matchRequest.message
            it[createdAt] = matchRequest.createdAt.toLocalDateTime()
            it[updatedAt] = matchRequest.updatedAt.toLocalDateTime()
        }
    }

    private fun insertAssignment(assignment: Assignment): Long {
        return AssignmentTable.insert {
            it[requestId] = assignment.requestId.value
            it[testerTeamId] = assignment.testerTeamId.value
            it[targetTeamId] = assignment.targetTeamId.value
            it[assignmentStatus] = assignment.status
            it[createdAt] = assignment.createdAt.toLocalDateTime()
            it[updatedAt] = assignment.updatedAt.toLocalDateTime()
        }[AssignmentTable.id]
    }

    private fun updateAssignment(
        assignmentId: Long,
        assignment: Assignment,
    ) {
        AssignmentTable.update(
            where = { AssignmentTable.id eq assignmentId },
        ) {
            it[requestId] = assignment.requestId.value
            it[testerTeamId] = assignment.testerTeamId.value
            it[targetTeamId] = assignment.targetTeamId.value
            it[assignmentStatus] = assignment.status
            it[createdAt] = assignment.createdAt.toLocalDateTime()
            it[updatedAt] = assignment.updatedAt.toLocalDateTime()
        }
    }

    private fun requestRowExists(requestId: Long): Boolean {
        return MatchRequestTable
            .selectAll()
            .where { MatchRequestTable.id eq requestId }
            .limit(1)
            .any()
    }

    private fun assignmentRowExists(assignmentId: Long): Boolean {
        return AssignmentTable
            .selectAll()
            .where { AssignmentTable.id eq assignmentId }
            .limit(1)
            .any()
    }

    companion object {
        private val ACTIVE_REQUEST_STATUSES = listOf(
            MatchRequestStatus.PENDING,
            MatchRequestStatus.ACCEPTED,
        )
    }
}
