package swm.calender.match.domain.model

import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.FeedbackId
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import java.time.Instant

data class FeedbackScores(
    val usability: Int,
    val value: Int,
    val reliability: Int,
    val recommendation: Int,
) {
    init {
        listOf(usability, value, reliability, recommendation).forEach { score ->
            if (score !in MIN_SCORE..MAX_SCORE) {
                throw MatchDomainException(MatchErrorMessage.FEEDBACK_SCORE_INVALID)
            }
        }
    }

    companion object {
        private const val MIN_SCORE = 1
        private const val MAX_SCORE = 5
    }
}

data class Feedback(
    val id: FeedbackId? = null,
    val assignmentId: AssignmentId,
    val submittedByTeamId: TeamId,
    val submittedByUserId: UserId,
    val scores: FeedbackScores,
    val summary: String,
    val improvementSuggestion: String?,
    val submittedAt: Instant,
) {
    init {
        validateSummary(summary)
        validateImprovementSuggestion(improvementSuggestion)
    }

    companion object {
        private const val SUMMARY_MIN_LENGTH = 10
        private const val SUMMARY_MAX_LENGTH = 1000
        private const val IMPROVEMENT_SUGGESTION_MAX_LENGTH = 1000

        fun submit(
            assignment: Assignment,
            submittedByTeamId: TeamId,
            submittedByUserId: UserId,
            scores: FeedbackScores,
            summary: String,
            improvementSuggestion: String?,
            submittedAt: Instant,
        ): Feedback {
            if (assignment.testerTeamId != submittedByTeamId) {
                throw MatchDomainException(MatchErrorMessage.FEEDBACK_SUBMITTER_NOT_TESTER_TEAM)
            }
            if (assignment.status != AssignmentStatus.ASSIGNED) {
                throw MatchDomainException(MatchErrorMessage.ASSIGNMENT_FEEDBACK_UNAVAILABLE)
            }

            return Feedback(
                assignmentId = assignment.requireId(),
                submittedByTeamId = submittedByTeamId,
                submittedByUserId = submittedByUserId,
                scores = scores,
                summary = summary.trim(),
                improvementSuggestion = improvementSuggestion?.trim()?.takeIf { it.isNotEmpty() },
                submittedAt = submittedAt,
            )
        }

        private fun validateSummary(summary: String) {
            if (summary.length !in SUMMARY_MIN_LENGTH..SUMMARY_MAX_LENGTH) {
                throw MatchDomainException(MatchErrorMessage.FEEDBACK_SUMMARY_LENGTH_INVALID)
            }
        }

        private fun validateImprovementSuggestion(improvementSuggestion: String?) {
            if (improvementSuggestion != null && improvementSuggestion.length > IMPROVEMENT_SUGGESTION_MAX_LENGTH) {
                throw MatchDomainException(MatchErrorMessage.FEEDBACK_IMPROVEMENT_SUGGESTION_TOO_LONG)
            }
        }
    }
}
