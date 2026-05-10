package swm.calender.core.api.controller.v1.match.service

import org.springframework.stereotype.Component
import swm.calender.core.api.controller.v1.match.request.FeedbackSubmitRequest
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.AssignmentId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.match.domain.model.FeedbackScores
import swm.calender.match.service.FeedbackService
import swm.calender.match.service.request.TeamTestHistoryGetRequest
import java.time.Instant
import swm.calender.match.service.request.FeedbackSubmitRequest as FeedbackSubmitServiceRequest
import swm.calender.match.service.response.FeedbackResponse as FeedbackServiceResponse
import swm.calender.match.service.response.FeedbackScoresResponse as FeedbackScoresServiceResponse
import swm.calender.match.service.response.TeamTestHistoryItemResponse as TeamTestHistoryItemServiceResponse
import swm.calender.match.service.response.TeamTestHistoryResponse as TeamTestHistoryServiceResponse

@Component
class FeedbackApiFacade(
    private val feedbackService: FeedbackService,
) {
    fun submitFeedback(
        user: AuthenticatedUser,
        assignmentId: Long,
        request: FeedbackSubmitRequest,
    ): FeedbackSnapshot {
        val scores = request.scores
        return FeedbackSnapshot.from(
            feedbackService.submitFeedback(
                FeedbackSubmitServiceRequest(
                    actorUserId = user.userId,
                    assignmentId = AssignmentId(assignmentId),
                    scores = FeedbackScores(
                        usability = scores.usability,
                        value = scores.value,
                        reliability = scores.reliability,
                        recommendation = scores.recommendation,
                    ),
                    summary = request.summary,
                    improvementSuggestion = request.improvementSuggestion,
                ),
            ),
        )
    }

    fun getTeamTestHistory(user: AuthenticatedUser): TeamTestHistorySnapshot {
        return TeamTestHistorySnapshot.from(
            feedbackService.getTeamTestHistory(
                TeamTestHistoryGetRequest(actorUserId = user.userId),
            ),
        )
    }
}

data class FeedbackScoresSnapshot(
    val usability: Int,
    val value: Int,
    val reliability: Int,
    val recommendation: Int,
) {
    companion object {
        fun from(response: FeedbackScoresServiceResponse): FeedbackScoresSnapshot {
            return FeedbackScoresSnapshot(
                usability = response.usability,
                value = response.value,
                reliability = response.reliability,
                recommendation = response.recommendation,
            )
        }
    }
}

data class FeedbackSnapshot(
    val feedbackId: Long,
    val assignmentId: Long,
    val submittedByTeamId: Long,
    val scores: FeedbackScoresSnapshot,
    val summary: String,
    val improvementSuggestion: String?,
    val submittedAt: Instant,
) {
    companion object {
        fun from(response: FeedbackServiceResponse): FeedbackSnapshot {
            return FeedbackSnapshot(
                feedbackId = response.feedbackId,
                assignmentId = response.assignmentId,
                submittedByTeamId = response.submittedByTeamId.value,
                scores = FeedbackScoresSnapshot.from(response.scores),
                summary = response.summary,
                improvementSuggestion = response.improvementSuggestion,
                submittedAt = response.submittedAt,
            )
        }
    }
}

data class TeamTestHistorySnapshot(
    val items: List<TeamTestHistoryItemSnapshot>,
) {
    companion object {
        fun from(response: TeamTestHistoryServiceResponse): TeamTestHistorySnapshot {
            return TeamTestHistorySnapshot(
                items = response.items.map(TeamTestHistoryItemSnapshot::from),
            )
        }
    }
}

data class TeamTestHistoryItemSnapshot(
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
        fun from(response: TeamTestHistoryItemServiceResponse): TeamTestHistoryItemSnapshot {
            return TeamTestHistoryItemSnapshot(
                assignmentId = response.assignmentId,
                campaignId = response.campaignId,
                serviceName = response.serviceName,
                testerTeamId = response.testerTeamId.value,
                targetTeamId = response.targetTeamId.value,
                assignmentStatus = response.assignmentStatus,
                feedbackSubmittedAt = response.feedbackSubmittedAt,
                feedbackSummary = response.feedbackSummary,
            )
        }
    }
}
