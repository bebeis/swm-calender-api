package swm.calender.core.api.controller.v1.match.service

import org.springframework.stereotype.Component
import swm.calender.core.api.controller.v1.match.request.MatchRequestCreateRequest
import swm.calender.core.api.controller.v1.match.request.MatchRequestStatusChangeRequest
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.RequestId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.core.enums.MatchRequestType
import swm.calender.match.service.MatchRequestService
import swm.calender.match.service.request.AssignmentGetRequest
import swm.calender.match.service.response.AssignmentResponse as AssignmentServiceResponse
import swm.calender.match.service.response.MatchRequestResponse as MatchRequestServiceResponse
import swm.calender.match.service.response.MatchRequestStatusChangeResponse as MatchRequestStatusChangeServiceResponse

@Component
class MatchRequestApiFacade(
    private val matchRequestService: MatchRequestService,
) {
    fun createRequest(
        user: AuthenticatedUser,
        campaignId: Long,
        request: MatchRequestCreateRequest,
    ): MatchRequestSnapshot {
        return MatchRequestSnapshot.from(
            matchRequestService.createRequest(
                swm.calender.match.service.request.MatchRequestCreateRequest(
                    actorUserId = user.userId,
                    campaignId = CampaignId(campaignId),
                    type = requireNotNull(request.type),
                    message = request.message,
                ),
            ),
        )
    }

    fun changeRequestStatus(
        user: AuthenticatedUser,
        requestId: Long,
        request: MatchRequestStatusChangeRequest,
    ): MatchRequestStatusChangeSnapshot {
        return MatchRequestStatusChangeSnapshot.from(
            matchRequestService.changeRequestStatus(
                swm.calender.match.service.request.MatchRequestStatusChangeRequest(
                    actorUserId = user.userId,
                    requestId = RequestId(requestId),
                    status = requireNotNull(request.status).domainStatus,
                ),
            ),
        )
    }

    fun getAssignment(
        user: AuthenticatedUser,
        assignmentId: Long,
    ): AssignmentSnapshot {
        return AssignmentSnapshot.from(
            matchRequestService.getAssignment(
                AssignmentGetRequest(
                    actorUserId = user.userId,
                    assignmentId = AssignmentId(assignmentId),
                ),
            ),
        )
    }
}

data class MatchRequestSnapshot(
    val requestId: Long,
    val campaignId: Long,
    val requestingTeamId: Long,
    val targetTeamId: Long,
    val type: MatchRequestType,
    val status: MatchRequestStatus,
) {
    companion object {
        fun from(response: MatchRequestServiceResponse): MatchRequestSnapshot {
            return MatchRequestSnapshot(
                requestId = response.requestId,
                campaignId = response.campaignId,
                requestingTeamId = response.requestingTeamId.value,
                targetTeamId = response.targetTeamId.value,
                type = response.type,
                status = response.status,
            )
        }
    }
}

data class MatchRequestStatusChangeSnapshot(
    val request: MatchRequestSnapshot,
    val assignmentId: Long?,
    val assignmentCreated: Boolean,
) {
    companion object {
        fun from(response: MatchRequestStatusChangeServiceResponse): MatchRequestStatusChangeSnapshot {
            return MatchRequestStatusChangeSnapshot(
                request = MatchRequestSnapshot.from(response.request),
                assignmentId = response.assignmentId,
                assignmentCreated = response.assignmentCreated,
            )
        }
    }
}

data class AssignmentSnapshot(
    val assignmentId: Long,
    val requestId: Long,
    val testerTeamId: Long,
    val targetTeamId: Long,
    val status: AssignmentStatus,
    val feedback: FeedbackSnapshot?,
) {
    companion object {
        fun from(response: AssignmentServiceResponse): AssignmentSnapshot {
            return AssignmentSnapshot(
                assignmentId = response.assignmentId,
                requestId = response.requestId,
                testerTeamId = response.testerTeamId.value,
                targetTeamId = response.targetTeamId.value,
                status = response.status,
                feedback = response.feedback?.let(FeedbackSnapshot::from),
            )
        }
    }
}
