package swm.calender.match.implement

import org.springframework.stereotype.Component
import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.TeamId
import swm.calender.match.domain.MatchRequestRepository
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.MatchRequest
import swm.calender.match.domain.model.Notification
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage

@Component
class MatchRequestReader(
    private val matchRequestRepository: MatchRequestRepository,
) {
    fun getRequest(requestId: RequestId): MatchRequest {
        return matchRequestRepository.findRequestById(requestId)
            ?: throw MatchDomainException(MatchErrorMessage.MATCH_REQUEST_NOT_FOUND)
    }

    fun ensureNoActiveRequest(
        campaignId: CampaignId,
        requestingTeamId: TeamId,
    ) {
        if (matchRequestRepository.existsActiveRequestByCampaignIdAndRequestingTeamId(campaignId, requestingTeamId)) {
            throw MatchDomainException(MatchErrorMessage.MATCH_REQUEST_DUPLICATED)
        }
    }

    fun findAssignmentByRequestId(requestId: RequestId): Assignment? {
        return matchRequestRepository.findAssignmentByRequestId(requestId)
    }

    fun getAssignment(assignmentId: AssignmentId): Assignment {
        return matchRequestRepository.findAssignmentById(assignmentId)
            ?: throw MatchDomainException(MatchErrorMessage.ASSIGNMENT_NOT_FOUND)
    }

    fun findNotificationsByTeamId(teamId: TeamId): List<Notification> {
        return matchRequestRepository.findNotificationsByTeamId(teamId)
    }
}
