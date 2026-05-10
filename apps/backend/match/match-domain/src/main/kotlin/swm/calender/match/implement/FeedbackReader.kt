package swm.calender.match.implement

import org.springframework.stereotype.Component
import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.TeamId
import swm.calender.match.domain.FeedbackRepository
import swm.calender.match.domain.TeamTestHistoryItem
import swm.calender.match.domain.model.Feedback
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage

@Component
class FeedbackReader(
    private val feedbackRepository: FeedbackRepository,
) {
    fun findByAssignmentId(assignmentId: AssignmentId): Feedback? {
        return feedbackRepository.findByAssignmentId(assignmentId)
    }

    fun ensureNoFeedback(assignmentId: AssignmentId) {
        if (feedbackRepository.existsByAssignmentId(assignmentId)) {
            throw MatchDomainException(MatchErrorMessage.FEEDBACK_DUPLICATED)
        }
    }

    fun findTeamTestHistory(teamId: TeamId): List<TeamTestHistoryItem> {
        return feedbackRepository.findTeamTestHistoryByTeamId(teamId)
    }
}
