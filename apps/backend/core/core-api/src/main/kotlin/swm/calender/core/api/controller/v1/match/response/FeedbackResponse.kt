package swm.calender.core.api.controller.v1.match.response

import swm.calender.core.api.controller.v1.match.service.FeedbackScoresSnapshot
import swm.calender.core.api.controller.v1.match.service.FeedbackSnapshot
import swm.calender.core.api.controller.v1.match.service.TeamTestHistoryItemSnapshot
import swm.calender.core.api.controller.v1.match.service.TeamTestHistorySnapshot
import swm.calender.core.enums.AssignmentStatus
import java.time.Instant

data class FeedbackScoresResponse(
    val usability: Int,
    val value: Int,
    val reliability: Int,
    val recommendation: Int,
) {
    companion object {
        fun from(snapshot: FeedbackScoresSnapshot): FeedbackScoresResponse {
            return FeedbackScoresResponse(
                usability = snapshot.usability,
                value = snapshot.value,
                reliability = snapshot.reliability,
                recommendation = snapshot.recommendation,
            )
        }
    }
}

data class FeedbackResponse(
    val feedbackId: Long,
    val assignmentId: Long,
    val submittedByTeamId: Long,
    val scores: FeedbackScoresResponse,
    val summary: String,
    val improvementSuggestion: String?,
    val submittedAt: Instant,
) {
    companion object {
        fun from(snapshot: FeedbackSnapshot): FeedbackResponse {
            return FeedbackResponse(
                feedbackId = snapshot.feedbackId,
                assignmentId = snapshot.assignmentId,
                submittedByTeamId = snapshot.submittedByTeamId,
                scores = FeedbackScoresResponse.from(snapshot.scores),
                summary = snapshot.summary,
                improvementSuggestion = snapshot.improvementSuggestion,
                submittedAt = snapshot.submittedAt,
            )
        }
    }
}

data class TeamTestHistoryResponse(
    val items: List<TeamTestHistoryItemResponse>,
) {
    companion object {
        fun from(snapshot: TeamTestHistorySnapshot): TeamTestHistoryResponse {
            return TeamTestHistoryResponse(
                items = snapshot.items.map(TeamTestHistoryItemResponse::from),
            )
        }
    }
}

data class TeamTestHistoryItemResponse(
    val assignmentId: Long,
    val campaignId: Long,
    val serviceName: String,
    val testerTeamId: Long,
    val targetTeamId: Long,
    val assignmentStatus: AssignmentStatus,
    val feedbackSubmittedAt: Instant?,
    val feedbackSummary: String?,
) {
    companion object {
        fun from(snapshot: TeamTestHistoryItemSnapshot): TeamTestHistoryItemResponse {
            return TeamTestHistoryItemResponse(
                assignmentId = snapshot.assignmentId,
                campaignId = snapshot.campaignId,
                serviceName = snapshot.serviceName,
                testerTeamId = snapshot.testerTeamId,
                targetTeamId = snapshot.targetTeamId,
                assignmentStatus = snapshot.assignmentStatus,
                feedbackSubmittedAt = snapshot.feedbackSubmittedAt,
                feedbackSummary = snapshot.feedbackSummary,
            )
        }
    }
}
