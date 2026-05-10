package swm.calender.match.domain

import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.TeamId
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.MatchRequest
import swm.calender.match.domain.model.MatchRequestStatusHistory
import swm.calender.match.domain.model.Notification

interface MatchRequestRepository {
    fun saveRequest(matchRequest: MatchRequest): MatchRequest

    fun findRequestById(requestId: RequestId): MatchRequest?

    fun existsActiveRequestByCampaignIdAndRequestingTeamId(
        campaignId: CampaignId,
        requestingTeamId: TeamId,
    ): Boolean

    fun saveStatusHistory(history: MatchRequestStatusHistory): MatchRequestStatusHistory

    fun saveAssignment(assignment: Assignment): Assignment

    fun findAssignmentByRequestId(requestId: RequestId): Assignment?

    fun findAssignmentById(assignmentId: AssignmentId): Assignment?

    fun saveNotification(notification: Notification): Notification
}
