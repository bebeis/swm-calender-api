package swm.calender.match.service.request

import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.UserId
import swm.calender.match.domain.model.FeedbackScores

data class FeedbackSubmitRequest(
    val actorUserId: UserId,
    val assignmentId: AssignmentId,
    val scores: FeedbackScores,
    val summary: String,
    val improvementSuggestion: String?,
)

data class TeamTestHistoryGetRequest(
    val actorUserId: UserId,
)
