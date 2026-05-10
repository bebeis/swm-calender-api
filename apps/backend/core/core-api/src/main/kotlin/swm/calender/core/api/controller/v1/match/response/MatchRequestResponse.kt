package swm.calender.core.api.controller.v1.match.response

import swm.calender.core.api.controller.v1.match.service.AssignmentSnapshot
import swm.calender.core.api.controller.v1.match.service.MatchRequestSnapshot
import swm.calender.core.api.controller.v1.match.service.MatchRequestStatusChangeSnapshot
import swm.calender.core.enums.AssignmentStatus
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.core.enums.MatchRequestType

data class MatchRequestResponse(
    val requestId: Long,
    val campaignId: Long,
    val requestingTeamId: Long,
    val targetTeamId: Long,
    val type: MatchRequestType,
    val status: MatchRequestStatus,
) {
    companion object {
        fun from(snapshot: MatchRequestSnapshot): MatchRequestResponse {
            return MatchRequestResponse(
                requestId = snapshot.requestId,
                campaignId = snapshot.campaignId,
                requestingTeamId = snapshot.requestingTeamId,
                targetTeamId = snapshot.targetTeamId,
                type = snapshot.type,
                status = snapshot.status,
            )
        }
    }
}

data class MatchRequestStatusChangeResponse(
    val request: MatchRequestResponse,
    val assignmentId: Long?,
    val assignmentCreated: Boolean,
) {
    companion object {
        fun from(snapshot: MatchRequestStatusChangeSnapshot): MatchRequestStatusChangeResponse {
            return MatchRequestStatusChangeResponse(
                request = MatchRequestResponse.from(snapshot.request),
                assignmentId = snapshot.assignmentId,
                assignmentCreated = snapshot.assignmentCreated,
            )
        }
    }
}

data class AssignmentResponse(
    val assignmentId: Long,
    val requestId: Long,
    val testerTeamId: Long,
    val targetTeamId: Long,
    val status: AssignmentStatus,
    val feedback: FeedbackResponse?,
) {
    companion object {
        fun from(snapshot: AssignmentSnapshot): AssignmentResponse {
            return AssignmentResponse(
                assignmentId = snapshot.assignmentId,
                requestId = snapshot.requestId,
                testerTeamId = snapshot.testerTeamId,
                targetTeamId = snapshot.targetTeamId,
                status = snapshot.status,
                feedback = snapshot.feedback?.let(FeedbackResponse::from),
            )
        }
    }
}
