package swm.calender.match.service.request

import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.core.enums.MatchRequestType

data class MatchRequestCreateRequest(
    val actorUserId: UserId,
    val campaignId: CampaignId,
    val type: MatchRequestType,
    val message: String?,
)

data class MatchRequestStatusChangeRequest(
    val actorUserId: UserId,
    val requestId: RequestId,
    val status: MatchRequestStatus,
)

data class AssignmentGetRequest(
    val actorUserId: UserId,
    val assignmentId: AssignmentId,
)
