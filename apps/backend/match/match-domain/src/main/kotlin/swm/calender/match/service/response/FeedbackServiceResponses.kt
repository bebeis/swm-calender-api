package swm.calender.match.service.response

import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.match.domain.TeamTestHistoryItem
import swm.calender.match.domain.model.Feedback
import swm.calender.match.domain.model.FeedbackScores
import java.time.Instant

data class FeedbackScoresResponse(
    val usability: Int,
    val value: Int,
    val reliability: Int,
    val recommendation: Int,
) {
    companion object {
        fun from(scores: FeedbackScores): FeedbackScoresResponse {
            return FeedbackScoresResponse(
                usability = scores.usability,
                value = scores.value,
                reliability = scores.reliability,
                recommendation = scores.recommendation,
            )
        }
    }
}

data class FeedbackResponse(
    val feedbackId: Long,
    val assignmentId: Long,
    val submittedByTeamId: TeamId,
    val scores: FeedbackScoresResponse,
    val summary: String,
    val improvementSuggestion: String?,
    val submittedAt: Instant,
) {
    companion object {
        fun from(feedback: Feedback): FeedbackResponse {
            return FeedbackResponse(
                feedbackId = requireNotNull(feedback.id).value,
                assignmentId = feedback.assignmentId.value,
                submittedByTeamId = feedback.submittedByTeamId,
                scores = FeedbackScoresResponse.from(feedback.scores),
                summary = feedback.summary,
                improvementSuggestion = feedback.improvementSuggestion,
                submittedAt = feedback.submittedAt,
            )
        }
    }
}

data class TeamTestHistoryResponse(
    val items: List<TeamTestHistoryItemResponse>,
) {
    companion object {
        fun from(items: List<TeamTestHistoryItem>): TeamTestHistoryResponse {
            return TeamTestHistoryResponse(
                items = items.map(TeamTestHistoryItemResponse::from),
            )
        }
    }
}

data class TeamTestHistoryItemResponse(
    val assignmentId: Long,
    val campaignId: Long,
    val serviceName: String,
    val testerTeamId: TeamId,
    val targetTeamId: TeamId,
    val assignmentStatus: AssignmentStatus,
    val feedbackSubmittedAt: Instant?,
    val feedbackSummary: String?,
) {
    companion object {
        fun from(item: TeamTestHistoryItem): TeamTestHistoryItemResponse {
            return TeamTestHistoryItemResponse(
                assignmentId = item.assignment.requireId().value,
                campaignId = item.campaignId.value,
                serviceName = item.serviceName,
                testerTeamId = item.assignment.testerTeamId,
                targetTeamId = item.assignment.targetTeamId,
                assignmentStatus = item.assignment.status,
                feedbackSubmittedAt = item.feedback?.submittedAt,
                feedbackSummary = item.feedback?.summary,
            )
        }
    }
}
