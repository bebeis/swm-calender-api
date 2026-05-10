package swm.calender.match.service.response

import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.core.enums.MatchRequestType
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.Feedback
import swm.calender.match.domain.model.MatchRequest

data class MatchRequestResponse(
    val requestId: Long,
    val campaignId: Long,
    val requestingTeamId: TeamId,
    val targetTeamId: TeamId,
    val type: MatchRequestType,
    val status: MatchRequestStatus,
) {
    companion object {
        fun from(matchRequest: MatchRequest): MatchRequestResponse {
            return MatchRequestResponse(
                requestId = requireNotNull(matchRequest.id).value,
                campaignId = matchRequest.campaignId.value,
                requestingTeamId = matchRequest.requestingTeamId,
                targetTeamId = matchRequest.targetTeamId,
                type = matchRequest.type,
                status = matchRequest.status,
            )
        }
    }
}

data class MatchRequestStatusChangeResponse(
    val request: MatchRequestResponse,
    val assignmentId: Long?,
    val assignmentCreated: Boolean,
)

data class AssignmentResponse(
    val assignmentId: Long,
    val requestId: Long,
    val testerTeamId: TeamId,
    val targetTeamId: TeamId,
    val status: AssignmentStatus,
    val feedback: FeedbackResponse?,
) {
    companion object {
        fun from(
            assignment: Assignment,
            feedback: Feedback?,
        ): AssignmentResponse {
            return AssignmentResponse(
                assignmentId = requireNotNull(assignment.id).value,
                requestId = assignment.requestId.value,
                testerTeamId = assignment.testerTeamId,
                targetTeamId = assignment.targetTeamId,
                status = assignment.status,
                feedback = feedback?.let(FeedbackResponse::from),
            )
        }
    }
}
